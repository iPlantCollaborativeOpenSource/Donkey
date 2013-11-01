(ns donkey.services.filesystem.actions
  (:require [cemerick.url :as url]
            [cheshire.core :as json]
            [clojure-commons.file-utils :as ft]
            [clojure.data.codec.base64 :as b64]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.java.io :as ds]
            [deliminator.core :as deliminator]
            [donkey.services.filesystem.riak :as riak]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.garnish.irods :as filetypes]
            [ring.util.codec :as cdc]
            [clj-jargon.lazy-listings :as ll]
            [clj-icat-direct.icat :as icat])
  (:use [clj-jargon.jargon :exclude [init list-dir] :as jargon]
        [clojure-commons.error-codes]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [slingshot.slingshot :only [try+ throw+]])
  (:import [org.apache.tika Tika]
           [au.com.bytecode.opencsv CSVReader]
           [java.util UUID]))

(defmacro log-rulers
  [cm users msg & body]
  `(let [result# (do ~@body)]
     (when (debug-ownership)
       (->> ~users
            (map #(when (jargon/one-user-to-rule-them-all? ~cm %)
                    (jargon/log-stack-trace (str ~msg " - " % " rules all"))))
            (dorun)))
     result#))

(defn format-call
  [fn-name & args]
  (with-open [w (java.io.StringWriter.)]
    (clojure.pprint/write (conj args (symbol fn-name)) :stream w)
    (str w)))

(defn filtered-user-perms
  [cm user abspath]
  (let [filtered-users (set (conj (fs-perms-filter) user (irods-user)))]
    (filter
     #(not (contains? filtered-users (:user %1)))
     (list-user-perms cm abspath))))

(defn- list-perm
  [cm user abspath]
  {:path abspath
   :user-permissions (filtered-user-perms cm user abspath)})

(defn list-perms
  [user abspaths]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "list-perms" user abspaths)
     (validators/user-exists cm user)
     (validators/all-paths-exist cm abspaths)
     (validators/user-owns-paths cm user abspaths)
     (mapv (partial list-perm cm user) abspaths))))

(defn create
  "Creates a directory at 'path' in iRODS and sets the user to 'user'.

   Parameters:
     user - String containing the username of the user requesting the
         directory.
     path - The path that the directory will be created at in iRODS.

   Returns a map of the format {:action \"create\" :path \"path\"}"
  [user path]
  (log/debug (str "create " user " " path))
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
      cm [user]
      (format-call "create" user path)
      (let [fixed-path (ft/rm-last-slash path)]
        (when-not (good-string? fixed-path)
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                   :path path}))
        (validators/user-exists cm user)
        (validators/path-writeable cm user (ft/dirname fixed-path))
        (validators/path-not-exists cm fixed-path)

        (mkdir cm fixed-path)
        (set-owner cm fixed-path user)
        {:path fixed-path :permissions (collection-perm-map cm user fixed-path)}))))

(defn source->dest
  [source-path dest-path]
  (ft/path-join dest-path (ft/basename source-path)))

(defn move-paths
  "Moves directories listed in 'sources' into the directory listed in 'dest'. This
   works by calling move and passing it move-dir."
  [user sources dest]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "move-paths" user sources dest)
     (let [path-list  (conj sources dest)
           all-paths  (apply merge (mapv #(hash-map (source->dest %1 dest) %1) sources))
           dest-paths (keys all-paths)
           sources    (mapv ft/rm-last-slash sources)
           dest       (ft/rm-last-slash dest)]
       (validators/user-exists cm user)
       (validators/all-paths-exist cm sources)
       (validators/all-paths-exist cm [dest])
       (validators/path-is-dir cm dest)
       (validators/user-owns-paths cm user sources)
       (validators/path-writeable cm user dest)
       (validators/no-paths-exist cm dest-paths)
       (move-all cm sources dest :user user :admin-users (irods-admins))
       {:sources sources :dest dest}))))



(defn- preview-buffer
  [cm path size]
  (let [realsize (file-size cm path)
        buffsize (if (<= realsize size) realsize size)
        buff     (char-array buffsize)]
    (read-file cm path buff)
    (.append (StringBuilder.) buff)))

(defn gen-preview
  [cm path size]
  (if (zero? (file-size cm path))
    ""
    (str (preview-buffer cm path size))))

(defn preview
  "Grabs a preview of a file in iRODS.

   Parameters:
     user - The username of the user requesting the preview.
     path - The path to the file in iRODS that will be previewed.
     size - The size (in bytes) of the preview to be created."
  [user path size]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (jargon-cfg) [cm]
      (log-rulers
        cm [user]
        (format-call "preview" user path size)
        (log/debug (str "preview " user " " path " " size))
        (validators/user-exists cm user)
        (validators/path-exists cm path)
        (validators/path-readable cm user path)
        (validators/path-is-file cm path)
        (gen-preview cm path size)))))

(defn fix-unit
  [avu]
  (if (= (:unit avu) IPCRESERVED)
    (assoc avu :unit "")
    avu))

(defn list-path-metadata
  [cm path]
  (filterv
   #(not= (:unit %) IPCSYSTEM)
   (map fix-unit (get-metadata cm (ft/rm-last-slash path)))))

(defn list-user-groups
  [user]
  "Returns a list of names for the groups a user is in.

   Parameters:
     user - the user's iRODS account name

   Returns:
     A list of group names

   Preconditions:
     clj-jargon must have been initialized

   Throws:
     ERR_NOT_A_USER - This is thrown if user is not a valid iRODS account name."
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "list-user-groups" user)
     (validators/user-exists cm user)
     (user-groups cm user))))

(defn reserved-unit
  "Turns a blank unit into a reserved unit."
  [avu-map]
  (if (string/blank? (:unit avu-map))
    IPCRESERVED
    (:unit avu-map)))

(defn metadata-get
  [user path]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "metadata-get" user path)
     (validators/user-exists cm user)
     (validators/path-exists cm path)
     (validators/path-readable cm user path)
     {:metadata (list-path-metadata cm path)})))

(defn metadata-set
  [user path avu-map]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "metadata-set" user path avu-map)
     (validators/user-exists cm user)

     (when (= "failure" (:status avu-map))
       (throw+ {:error_code ERR_INVALID_JSON}))

     (validators/path-exists cm path)
     (validators/path-writeable cm user path)

     (let [fixed-path (ft/rm-last-slash path)
           new-unit   (reserved-unit avu-map)
           attr       (:attr avu-map)
           value      (:value avu-map)]
       (log/warn "Fixed Path:" fixed-path)
       (log/warn "check" (true? (attr-value? cm fixed-path attr value)))
       (when-not (attr-value? cm fixed-path attr value)
         (log/warn "Adding " attr value "to" fixed-path)
         (set-metadata cm fixed-path attr value new-unit))
       {:path fixed-path :user user}))))

(defn encode-str
  [str-to-encode]
  (String. (b64/encode (.getBytes str-to-encode))))

(defn workaround-delete
  "Gnarly workaround for a bug (I think) in Jargon. If a value
   in an AVU is formatted a certain way, it can't be deleted.
   We're base64 encoding the value before deletion to ensure
   that the deletion will work."
  [cm path attr]
  (let [{:keys [attr value unit]} (first (get-attribute cm path attr))]
    (set-metadata cm path attr (encode-str value) unit)))

(defn metadata-batch-set
  [user path adds-dels]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "metadata-batch-set" user path adds-dels)
     (validators/user-exists cm user)
     (validators/path-exists cm path)
     (validators/path-writeable cm user path)

     (let [new-path (ft/rm-last-slash path)]
       (doseq [del (:delete adds-dels)]
         (when (attribute? cm new-path del)
           (workaround-delete cm new-path del)
           (delete-metadata cm new-path del)))

       (doseq [avu (:add adds-dels)]
         (let [new-unit (reserved-unit avu)
               attr     (:attr avu)
               value    (:value avu)]
           (if-not (attr-value? cm new-path attr value)
             (set-metadata cm new-path attr value new-unit))))
       {:path new-path :user user}))))

(defn metadata-delete
  [user path attr]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "metadata-delete" user path attr)
     (validators/user-exists cm user)
     (validators/path-exists cm path)
     (validators/path-writeable cm user path)

     (let [fix-unit #(if (= (:unit %1) IPCRESERVED) (assoc %1 :unit "") %1)
           avu      (map fix-unit (get-metadata cm (ft/rm-last-slash path)))]
       (workaround-delete cm path attr)
       (delete-metadata cm path attr))
     {:path path :user user})))

(defn url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn url-decode
  [string-to-decode]
  (if (url-encoded? string-to-decode)
    (url/url-decode string-to-decode)
    string-to-decode))

(defn path-exists?
  ([path]
     (path-exists? "" path))
  ([user path]
    (let [path (ft/rm-last-slash path)]
      (with-jargon (jargon-cfg) [cm]
        (log-rulers
          cm [user]
          (format-call "path-exists?" user path)
          (exists? cm (url-decode path)))))))

(defn path-is-dir?
  [path]
  (let [path (url-decode path)]
    (with-jargon (jargon-cfg) [cm]
      (and (exists? cm path) (is-dir? cm path)))))

(defn path-is-file?
  [path]
  (let [path (url-decode path)]
    (with-jargon (jargon-cfg) [cm]
      (and (exists? cm path) (is-file? cm path)))))

(defn count-shares
  [cm user path]
  (let [filter-users (set (conj (fs-perms-filter) user (irods-user)))
        full-listing (list-user-perms cm path)]
    (count
     (filterv
      #(not (contains? filter-users (:user %1)))
      full-listing))))

(defn merge-counts
  [stat-map cm user path]
  (if (is-dir? cm path)
    (merge stat-map {:file-count (icat/number-of-files-in-folder user path)
                     :dir-count  (icat/number-of-folders-in-folder user path)})
    stat-map))

(defn merge-shares
  [stat-map cm user path]
  (if (owns? cm user path)
    (merge stat-map {:share-count (count-shares cm user path)})
    stat-map))

(defn merge-type-info
  [stat-map cm user path]
  (if-not (is-dir? cm path)
    (-> stat-map
      (merge {:info-type (filetypes/get-types cm user path)})
      (merge {:mime-type (.detect (Tika.) (input-stream cm path))}))
    stat-map))

(defn path-stat
  [user path]
  (let [path (ft/rm-last-slash path)]
    (log/warn "[path-stat] user:" user "path:" path)
    (with-jargon (jargon-cfg) [cm]
      (log-rulers
        cm [user]
        (format-call "path-stat" user path)
        (validators/path-exists cm path)
        (-> (stat cm path)
          (merge {:permissions (permissions cm user path)})
          (merge-type-info cm user path)
          (merge-shares cm user path)
          (merge-counts cm user path))))))

(defn- format-tree-urls
  [treeurl-maps]
  (if (pos? (count treeurl-maps))
    (json/decode (:value (first (seq treeurl-maps))) true)
    []))

(defn preview-url
  [user path]
  (str "file/preview?user=" (cdc/url-encode user) "&path=" (cdc/url-encode path)))

(defn content-type
  [cm path]
  (.detect (Tika.) (input-stream cm path)))

(defn extract-tree-urls
  [cm fpath]
  (if (attribute? cm fpath "tree-urls")
    (-> (get-attribute cm fpath "tree-urls")
        first
        :value
        riak/get-tree-urls
        (json/decode true)
        :tree-urls)
    []))

(defn manifest
  [user path data-threshold]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (jargon-cfg) [cm]
      (log-rulers
        cm [user]
        (format-call "manifest" user path data-threshold)
        (validators/user-exists cm user)
        (validators/path-exists cm path)
        (validators/path-is-file cm path)
        (validators/path-readable cm user path)

        {:action       "manifest"
         :content-type (content-type cm path)
         :tree-urls    (extract-tree-urls cm path)
         :info-type    (filetypes/get-types cm user path)
         :mime-type    (.detect (Tika.) (input-stream cm path))
         :preview      (preview-url user path)}))))

(defn tika-detect-type
  [user file-path]
  (with-jargon (jargon-cfg) [cm-new]
    (validators/user-exists cm-new user)
    (validators/path-exists cm-new file-path)
    (validators/path-readable cm-new user file-path)
    (.detect (Tika.) (input-stream cm-new file-path))))

(defn download-file
  [user file-path]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm file-path)
    (validators/path-readable cm user file-path)
    
    (if (zero? (file-size cm file-path)) "" (input-stream cm file-path))))

(defn download
  [user filepaths]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "download" user filepaths)
     (validators/user-exists cm user)

     (let [cart-key (str (System/currentTimeMillis))
           account  (:irodsAccount cm)]
       {:action "download"
        :status "success"
        :data
        {:user user
         :home (ft/path-join "/" (irods-zone) "home" user)
         :password (store-cart cm user cart-key filepaths)
         :host (.getHost account)
         :port (.getPort account)
         :zone (.getZone account)
         :defaultStorageResource (.getDefaultStorageResource account)
         :key cart-key}}))))

(defn upload
  [user]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "upload" user)
     (validators/user-exists cm user)

     (let [account (:irodsAccount cm)]
       {:action "upload"
        :status "success"
        :data
        {:user user
         :home (ft/path-join "/" (irods-zone) "home" user)
         :password (temp-password cm user)
         :host (.getHost account)
         :port (.getPort account)
         :zone (.getZone account)
         :defaultStorageResource (.getDefaultStorageResource account)
         :key (str (System/currentTimeMillis))}}))))

(def shared-with-attr "ipc-contains-obj-shared-with")

(defn delete-avu
  "Deletes the provided AVU from the path."
  [cm fpath avu-map]
  (.deleteAVUMetadata (:collectionAO cm) fpath (map2avu avu-map)))

(defn add-user-shared-with
  "Adds 'ipc-contains-obj-shared-with' AVU for a user to an object if it's not there."
  [cm fpath shared-with]
  (when (empty? (get-avus-by-collection cm fpath shared-with shared-with-attr))
    (set-metadata cm fpath shared-with shared-with shared-with-attr)))

(defn remove-user-shared-with
  "Removes 'ipc-contains-obj-shared-with' AVU for a user from an object if it's there."
  [cm fpath shared-with]
  (when-not (empty? (get-avus-by-collection cm fpath shared-with shared-with-attr))
    (delete-metadata cm fpath shared-with)))

(defn shared?
  ([cm share-with fpath]
     (:read (permissions cm share-with fpath)))
  ([cm share-with fpath desired-perms]
     (let [curr-perms (permissions cm share-with fpath)]
       (and (:read curr-perms) (= curr-perms desired-perms)))))

(defn- skip-share
  [user path reason]
  (log/warn "Skipping share of" path "with" user "because:" reason)
  {:user    user
   :path    path
   :reason  reason
   :skipped true})

(defn- share-path-home
  "Returns the home directory that a shared file is under."
  [share-path]
  (string/join "/" (take 4 (string/split share-path #"\/"))))

(defn- share-path
  "Shares a path with a user. This consists of the following steps:

       1. The parent directories up to the sharer's home directory need to be marked as readable
          by the sharee. Othwerwise, any files that are shared will be orphaned in the UI.

       2. If the shared item is a directory then the inherit bit needs to be set so that files
          that are uploaded into the directory will also be shared.

       3. The permissions are set on the item being shared. This is done recursively in case the
          item being shared is a directory."
  [cm user share-with {read-perm :read write-perm :write own-perm :own :as perms} fpath]
  (let [hdir      (share-path-home fpath)
        trash-dir (trash-base-dir cm user)
        base-dirs #{hdir trash-dir}]
    (log/warn fpath "is being shared with" share-with "by" user)
    (process-parent-dirs (partial set-readable cm share-with true) #(not (base-dirs %)) fpath)

    (when (is-dir? cm fpath)
      (log/warn fpath "is a directory, setting the inherit bit.")
      (.setAccessPermissionInherit (:collectionAO cm) (:zone cm) fpath true))

    (log/warn share-with "is being given read permissions on" hdir "by" user)
    (set-permissions cm share-with hdir true false false false)

    (set-permissions cm share-with fpath read-perm write-perm own-perm true)
    (log/warn
      share-with
      "is being given recursive permissions ("
      "read:" read-perm
      "write:" write-perm
      "own:" own-perm ")"
      "on" fpath)

    {:user share-with :path fpath}))

(defn- share-paths
  [cm user share-withs fpaths perms]
  (for [share-with share-withs
        fpath      fpaths]
    (cond (= user share-with)                 (skip-share share-with fpath :share-with-self)
          (in-trash? cm user fpath)           (skip-share share-with fpath :share-from-trash)
          (shared? cm share-with fpath perms) (skip-share share-with fpath :already-shared)
          :else                               (share-path cm user share-with perms fpath))))

(defn share
  [user share-withs fpaths perms]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm (conj share-withs user)
     (format-call "share" user share-withs fpaths perms)
     (validators/user-exists cm user)
     (validators/all-users-exist cm share-withs)
     (validators/all-paths-exist cm fpaths)
     (validators/user-owns-paths cm user fpaths)

     (let [keyfn      #(if (:skipped %) :skipped :succeeded)
           share-recs (group-by keyfn (share-paths cm user share-withs fpaths perms))
           sharees    (map :user (:succeeded share-recs))
           home-dir   (user-home-dir user)]
       (dorun (map (partial add-user-shared-with cm (user-home-dir user)) sharees))
       {:user        sharees
        :path        fpaths
        :skipped     (map #(dissoc % :skipped) (:skipped share-recs))
        :permissions perms}))))

(defn contains-subdir?
  [cm dpath]
  (some #(is-dir? cm %) (list-paths cm dpath)))

(defn subdirs
  [cm dpath]
  (filter #(is-dir? cm %) (list-paths cm dpath)))

(defn some-subdirs-readable?
  [cm user parent-path]
  (some #(is-readable? cm user %1) (subdirs cm parent-path)))

(defn- remove-inherit-bit?
  [cm user fpath]
  (empty? (remove (comp (conj (set (irods-admins)) user) :user)
                  (list-user-perms cm fpath))))

(defn- unshare-dir
  "Removes the inherit bit from a directory if the directory is no longer shared with any accounts
   other than iRODS administrative accounts."
  [cm user unshare-with fpath]
  (when (remove-inherit-bit? cm user fpath)
    (log/warn "Removing inherit bit on" fpath)
    (.setAccessPermissionToNotInherit (:collectionAO cm) (:zone cm) fpath true)))

(defn- unshare-path
  "Removes permissions for a user to access a path.  This consists of several steps:

       1. Remove the access permissions for the user.  This is done recursively in case the path
          being unshared is a directory.

       2. If the item being unshared is a directory, perform any directory-specific unsharing
          steps that are required.

       3. Remove the user's read permissions for parent directories in which the user no longer has
          access to any other files or subdirectories."
  [cm user unshare-with fpath]
  (let [base-dirs #{(ft/rm-last-slash (user-home-dir user)) (trash-base-dir cm user)}]
    (log/warn "Removing permissions on" fpath "from" unshare-with "by" user)
    (remove-permissions cm unshare-with fpath)

    (when (is-dir? cm fpath)
      (log/warn "Unsharing directory" fpath "from" unshare-with "by" user)
      (unshare-dir cm user unshare-with fpath))

    (log/warn "Removing read perms on parents of" fpath "from" unshare-with "by" user)
    (process-parent-dirs
      (partial set-readable cm unshare-with false)
      #(and (not (base-dirs %)) (not (contains-accessible-obj? cm unshare-with %)))
      fpath)
    {:user unshare-with :path fpath}))

(defn- unshare-paths
  [cm user unshare-withs fpaths]
  (for [unshare-with unshare-withs
        fpath        fpaths]
    (cond (= user unshare-with)           (skip-share unshare-with fpath :unshare-with-self)
          (shared? cm unshare-with fpath) (unshare-path cm user unshare-with fpath)
          :else                           (skip-share unshare-with fpath :not-shared))))

(defn clean-up-unsharee-avus
  [cm fpath unshare-with]
  (when-not (shared? cm unshare-with fpath)
    (log/warn "Removing shared with AVU on" fpath "for" unshare-with)
    (remove-user-shared-with cm fpath unshare-with)))

(defn unshare
  "Allows 'user' to unshare file 'fpath' with user 'unshare-with'."
  [user unshare-withs fpaths]
  (log/debug "entered unshare")

  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm (conj unshare-withs user)
     (format-call "unshare" user unshare-withs fpaths)
     (validators/user-exists cm user)
     (validators/all-users-exist cm unshare-withs)
     (validators/all-paths-exist cm fpaths)
     (validators/user-owns-paths cm user fpaths)

     (log/debug "unshare - after validators")
     (log/debug "unshare - user: " user)
     (log/debug "unshare - unshare-withs: " unshare-withs)
     (log/debug "unshare - fpaths: " fpaths)

     (let [keyfn        #(if (:skipped %) :skipped :succeeded)
           unshare-recs (group-by keyfn (unshare-paths cm user unshare-withs fpaths))
           unsharees    (map :user (:succeeded unshare-recs))
           home-dir     (user-home-dir user)]
       (dorun (map (partial clean-up-unsharee-avus cm home-dir) unsharees))
       {:user unsharees
        :path fpaths
        :skipped (map #(dissoc % :skipped) (:skipped unshare-recs))}))))

(defn list-of-homedirs-with-shared-files
  [cm user]
  (mapv
   #(let [stat (.getObjStat (:fileSystemAO cm) %1)]
      (hash-map
       :id            %1
       :label         (id->label cm user %1)
       :hasSubDirs    true
       :date-created  (date-created-from-stat stat)
       :date-modified (date-mod-from-stat stat)
       :permissions   (collection-perm-map cm user %1)
       :file-size     (size-from-stat stat)))
   (filterv
    #(is-readable? cm user %1)
    (list-collections-with-attr-units cm user shared-with-attr))))

(defn list-sharing
  [cm user path]
  (log/warn "entered list-sharing")
  (let [dirs (list-of-homedirs-with-shared-files cm user)]
    (hash-map
     :id            path
     :label         (id->label cm user path)
     :hasSubDirs    true
     :date-created  (created-date cm path)
     :date-modified (lastmod-date cm path)
     :permissions   (collection-perm-map cm user path)
     :folders       dirs)))

(defn sharing-data
  [cm user root-dir]
  (list-sharing cm user (ft/rm-last-slash root-dir)))

(defn shared-root-listing
  [user root-dir inc-files filter-files]

  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "shared-root-listing" user root-dir inc-files filter-files)
     (when-not (is-readable? cm user root-dir)
       (log/warn "Setting read perms on" (ft/rm-last-slash root-dir) "for" user)
       (set-permissions cm user (ft/rm-last-slash root-dir) true false false))

     (let [listing (sharing-data cm user root-dir)]
       (assoc listing :label (id->label cm user (:id listing)))))))

(defn get-quota
  [user]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "get-quota" user)
     (validators/user-exists cm user)
     (quota cm user))))

(defn copy-path
  ([copy-map]
     (copy-path copy-map "ipc-de-copy-from"))

  ([{:keys [user from to]} copy-key]
     (with-jargon (jargon-cfg) [cm]
       (log-rulers
        cm [user]
        (format-call "copy-path" {:user user :from from :to to} copy-key)
        (validators/user-exists cm user)
        (validators/all-paths-exist cm from)
        (validators/all-paths-readable cm user from)
        (validators/path-exists cm to)
        (validators/path-writeable cm user to)
        (validators/path-is-dir cm to)
        (validators/no-paths-exist cm (mapv #(ft/path-join to (ft/basename %)) from))

        (when (some true? (mapv #(= to %1) from))
          (throw+ {:error_code ERR_INVALID_COPY
                   :paths (filterv #(= to %1) from)}))

        (doseq [fr from]
          (let [metapath (ft/rm-last-slash (ft/path-join to (ft/basename fr)))]
            (copy cm fr to)
            (set-metadata cm metapath copy-key fr "")
            (set-owner cm to user)))

        {:sources from :dest to}))))

(defn- ticket-uuids?
  [cm user new-uuids]
  (try+
    (validators/all-tickets-nonexistant cm user new-uuids)
    true
    (catch error? e false)))

(defn- gen-uuids
  [cm user num-uuids]
  (let [new-uuids (doall (repeatedly num-uuids #(string/upper-case (str (UUID/randomUUID)))))]
    (if (ticket-uuids? cm user new-uuids)
      new-uuids
      (recur cm user num-uuids)) ))

(defn add-tickets
  [user paths public?]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "add-tickets" user paths public?)
     (let [new-uuids (gen-uuids cm user (count paths))] 
       (validators/user-exists cm user)
       (validators/all-paths-exist cm paths)
       (validators/all-paths-writeable cm user paths)
       
       (doseq [[path uuid] (map list paths new-uuids)]
         (log/warn "[add-tickets] adding ticket for " path "as" uuid)
         (create-ticket cm (:username cm) path uuid)
         (when public?
           (log/warn "[add-tickets] making ticket" uuid "public")
           (doto (ticket-admin-service cm (:username cm))
             (.addTicketGroupRestriction uuid "public"))))
     
       {:user user :tickets (mapv #(ticket-map cm (:username cm) %) new-uuids)}))))

(defn remove-tickets
  [user ticket-ids]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "remove-tickets" user ticket-ids)
     (validators/user-exists cm user)
     (validators/all-tickets-exist cm user ticket-ids)

     (let [all-paths (mapv #(.getIrodsAbsolutePath (ticket-by-id cm (:username cm) %)) ticket-ids)]
       (validators/all-paths-writeable cm user all-paths)
       (doseq [ticket-id ticket-ids]
         (delete-ticket cm (:username cm) ticket-id))
       {:user user :tickets ticket-ids}))))

(defn list-tickets-for-paths
  [user paths]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "list-tickets-for-paths" user paths)
     (validators/user-exists cm user)
     (validators/all-paths-exist cm paths)
     (validators/all-paths-readable cm user paths)

     {:tickets
      (apply merge (mapv #(hash-map %1 (ticket-ids-for-path cm (:username cm) %1)) paths))})))

(defn paths-contain-char
  [paths char]
  (when-not (good-string? char)
    (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
             :character char}))

  (apply merge (map #(hash-map %1 (not (nil? (re-seq (re-pattern char) %1)))) paths)))

(defn parent-dirs
  [user path]
  (let [pdirs (atom [])]
    (process-parent-dirs
     #(reset! pdirs (conj @pdirs %1))
     #(and (not (nil? %1))
           (not (= %1 (user-home-dir user)))) path)
    @pdirs))

(defn all-parent-dirs
  [user paths]
  (sort-by
   #(count (string/split %1 #"/")) >
   (vec (apply set/union (map #(set (parent-dirs user %1)) paths)))))

(defn looped-new-name
  "Iterates over the path, appending a _# to the end until a path that doesn't already exist is
   found."
  [cm path new-char]
  (loop [idx 0]
    (let [new-path (string/replace path #" " (str new-char "_" idx))]
      (if-not (exists? cm new-path)
        new-path
        (recur (inc idx))))))

(defn new-name
  "Creates a new name for the given path by replacing all spaces with the provided new-char.
   If the path is indicated to be a parent with the :parent flag, then the new name will NOT have
   a _# appended to it if the name already exists."
  [cm path new-char & {:keys [parent] :or {parent false}}]
  (let [new-path (string/replace path #" " new-char)]
    (if (or parent (not (exists? cm new-path)))
      new-path
      (looped-new-name cm path new-char))))

(defn has-space?
  "Returns a truthy value if the path contains a space somewhere in it."
  [path]
  (re-seq (re-pattern " ") path))

(defn move-spacey-path
  "Takes in a path and a new-char, replaces all spaces in the path with new-char, and returns a map
   with the original path as the key and the new path as the value. If the path is a parent, then
   the new name will not have a _# appended to it if it already exists."
  [cm user path new-char & {:keys [parent] :or {parent false}}]
  (when (has-space? (ft/basename path))
    (let [new-basename (new-name cm (ft/basename path) new-char :parent parent)
          new-path     (ft/path-join (ft/dirname path) new-basename)]
      (if (and (not (exists? cm new-path)) (exists? cm path))
        (move cm path new-path :user user :admin-users (irods-admins)))
      {path new-path})))

(defn fix-return-map
  [retmap new-char]
  (into {} (map #(hash-map (first %1) (string/replace (last %1) #" " new-char)) (seq retmap))))

(defn replace-spaces
  "Generates new paths by replacing all spaces with new-char."
  [user paths new-char]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "replace-spaces" paths new-char)
     (validators/user-exists cm user)
     (validators/all-paths-exist cm paths)
     (validators/user-owns-paths cm user paths)

     (when-not (good-string? new-char)
       (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                :character new-char}))

     (let [parent-dirs (all-parent-dirs user paths)]
       (validators/user-owns-paths cm user parent-dirs)

       (let [mv-base         #(move-spacey-path cm user %1 new-char :parent false)
             mv-parent       #(move-spacey-path cm user %1 new-char :parent true)
             basename-merges (apply merge (map mv-base paths))
             parent-merges   (apply merge (map mv-parent parent-dirs))]
         {:paths (fix-return-map basename-merges new-char)})))))

(defn read-file-chunk
  "Reads a chunk of a file starting at 'position' and reading a chunk of length 'chunk-size'."
  [user path position chunk-size]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "read-file-chunk" user path position chunk-size)
     (validators/user-exists cm user)
     (validators/path-exists cm path)
     (validators/path-is-file cm path)
     (validators/path-readable cm user path)

     {:path       path
      :user       user
      :start      (str position)
      :chunk-size (str chunk-size)
      :file-size  (str (file-size cm path))
      :chunk      (read-at-position cm path position chunk-size)})))

(defn overwrite-file-chunk
  "Writes a chunk of a file starting at 'position' and extending to the length of the string."
  [user path position update-string]
  (with-jargon (jargon-cfg) [cm]
    (log-rulers
     cm [user]
     (format-call "overwrite-file-chunk" user path position update-string)
     (validators/user-exists cm user)
     (validators/path-exists cm path)
     (validators/path-is-file cm path)
     (validators/path-writeable cm user path)
     (overwrite-at-position cm path position update-string)
     {:path       path
      :user       user
      :start      (str position)
      :chunk-size (str (count (.getBytes update-string)))
      :file-size  (str (file-size cm path))})))

(defn- closest-page
  [page-positions page-number]
  (let [idx (dec page-number)
        len (count page-positions)]
    (if (<= page-number len)
      [(page-positions idx) page-number]
      [(last page-positions) len])))

(defn- csv-page-result
  [path user delim file-size chunk-size page-positions page csv]
  {:path           path
   :user           user
   :delim          (str delim)
   :file-size      (str file-size)
   :chunk-size     (str chunk-size)
   :page-positions (mapv str page-positions)
   :page           (str page)
   :csv            csv})

(defn get-csv-page
  "Retrieves a CSV page for a given chunk size. `delim` is the character that is used as a field
   separator in the file. `page-positions` is a vector of positions of pages within the file,
   which is used as an optimization when retrieving a CSV page. Without it, it would be necessary
   to sequentially scan for the requested page with every call. `page-number` is the requsted page
   number. `chunk-size` is the maximum size of a page."
  [user path delim page-positions page-number chunk-size]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-is-file cm path)
    (validators/path-readable cm user path)

    (let [size       (file-size cm path)
          get-chunk  (fn [pos] (read-at-position cm path pos chunk-size))
          parse-page (fn [chunk] (deliminator/parse-excerpt chunk delim))
          get-page   (comp parse-page get-chunk)
          add-pos    (fn [ps p] (if (> p (last ps)) (conj ps p) ps))
          build-res  (partial csv-page-result path user delim size chunk-size)]
      (loop [[pos page] (closest-page page-positions page-number)
             positions  page-positions
             [csv len]  (get-page pos)]
        (let [next-pos  (+ pos len)
              positions (add-pos positions next-pos)]
          (cond (= page page-number) (build-res positions page csv)
                (< next-pos size)    (recur [next-pos (inc page)] positions (get-page next-pos))
                :else                (build-res positions page csv)))))))

(defn trim-to-line-start
  [str-chunk line-ending]
  (let [line-pos (.indexOf str-chunk line-ending)]
    (if (<= line-pos 0)
      str-chunk
      (.substring str-chunk (+ line-pos 1)))))

(defn calc-start-pos
  "Calculates the new start position after (trim-to-line-start) has been called."
  [start-pos orig-chunk trimmed-chunk]
  (+ start-pos (- (count (.getBytes orig-chunk)) (count (.getBytes trimmed-chunk)))))

(defn trim-to-last-line
  [str-chunk line-ending]
  (let [calced-pos (- (.lastIndexOf str-chunk line-ending) 1)
        last-pos   (if-not (pos? calced-pos) 1 calced-pos)]
    (.substring str-chunk 0 last-pos)))

(defn calc-end-pos
  "Calculates the new ending byte based on the start position and the current size of the chunk."
  [start-pos trimmed-chunk]
  (+ start-pos (- (count (.getBytes trimmed-chunk)) 1)))

(defn read-csv
  [separator csv-str]
  (let [ba  (java.io.ByteArrayInputStream. (.getBytes csv-str))
        isr (java.io.InputStreamReader. ba "UTF-8")]
    (mapv vec (.readAll (CSVReader. isr (.charAt separator 0))))))

(defn read-csv-chunk
  "Reads a chunk of a file and parses it as a CSV. The position and chunk-size are not guaranteed, since
   we shouldn't try to parse partial rows. We scan forward from the starting position to find the first
   line-ending and then scan backwards from the last position for the last line-ending."
  [user path position chunk-size line-ending separator]
  (with-jargon (jargon-cfg) [cm]
    (log/warn "[read-csv-chunk]" user path position chunk-size line-ending separator)
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-is-file cm path)
    (validators/path-readable cm user path)

    (when-not (contains? #{"\r\n" "\n"} line-ending)
      (throw+ {:error_code "ERR_INVALID_LINE_ENDING"
               :line-ending line-ending}))

    (let [chunk         (read-at-position cm path position chunk-size)
          front-trimmed (trim-to-line-start chunk line-ending)
          new-start-pos (calc-start-pos position chunk front-trimmed)
          trimmed-chunk (trim-to-last-line front-trimmed line-ending)
          new-end-pos   (calc-end-pos position trimmed-chunk)
          the-csv       (read-csv separator trimmed-chunk)]
      {:path       path
       :user       user
       :max-cols   (str (reduce #(if (>= %1 %2) %1 %2) (map count the-csv)))
       :start      (str new-start-pos)
       :end        (str new-end-pos)
       :chunk-size (str (count (.getBytes trimmed-chunk)))
       :file-size  (str (file-size cm path))
       :csv        the-csv})))

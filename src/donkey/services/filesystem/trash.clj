(ns donkey.services.filesystem.trash
  (:use [clojure-commons.error-codes] 
        [donkey.util.config]
        [donkey.util.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.jargon]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.actions :as irods-actions]))

(def alphanums (concat (range 48 58) (range 65 91) (range 97 123)))

(defn- trim-leading-slash
  [str-to-trim]
  (string/replace-first str-to-trim #"^\/" ""))

(defn- rand-str
  [length]
  (apply str (take length (repeatedly #(char (rand-nth alphanums))))))

(defn- randomized-trash-path
  [cm user path-to-inc]
  (ft/path-join
   (user-trash-path cm user)
   (str (ft/basename path-to-inc) "." (rand-str 7))))

(defn- move-to-trash
  [cm p user]
  (let [trash-path (randomized-trash-path cm user p)]
    (move cm p trash-path :user user :admin-users (irods-admins))
    (set-metadata cm trash-path "ipc-trash-origin" p IPCSYSTEM)))

(defn- delete-paths
  [user paths]
  (let [home-matcher #(= (str "/" (irods-zone) "/home/" user)
                         (ft/rm-last-slash %1))]
    (with-jargon (jargon-cfg) [cm]
      (let [paths (mapv ft/rm-last-slash paths)]
        (validators/user-exists cm user)
        (validators/all-paths-exist cm paths)
        (validators/user-owns-paths cm user paths)
        
        (when (some true? (mapv home-matcher paths))
          (throw+ {:error_code ERR_NOT_AUTHORIZED
                   :paths (filterv home-matcher paths)}))
        
        (doseq [p paths]
          (log/debug "path" p)
          (log/debug "readable?" user (owns? cm user p))
          
          (let [path-tickets (mapv :ticket-id (ticket-ids-for-path cm (:username cm) p))]
            (doseq [path-ticket path-tickets]
              (delete-ticket cm (:username cm) path-ticket)))
          
          (if-not (.startsWith p (user-trash-path cm user))
            (move-to-trash cm p user)
            (delete cm p)))

         {:paths paths}))))

(defn- trash-relative-path
  [path name user-trash]
  (trim-leading-slash
   (ft/path-join
    (or (ft/dirname (string/replace-first path (ft/add-trailing-slash user-trash) ""))
        "")
    name)))

(defn- user-trash
  [user]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    {:trash (user-trash-path cm user)}))

(defn- trash-origin-path
  [cm user p]
  (if (attribute? cm p "ipc-trash-origin")
    (:value (first (get-attribute cm p "ipc-trash-origin")))
    (ft/path-join (user-home-dir user) (ft/basename p))))

(defn- restore-to-homedir?
  [cm p]
  (not (attribute? cm p "ipc-trash-origin")))

(defn- restoration-path
  [cm user path]
  (let [user-home   (user-home-dir user)
        origin-path (trash-origin-path cm user path)
        inc-path    #(str origin-path "." %)]
    (if-not (exists? cm origin-path)
      origin-path
      (loop [attempts 0]
        (if (exists? cm (inc-path attempts))
          (recur (inc attempts))
          (inc-path attempts))))))

(defn- restore-parent-dirs
  [cm user path]
  (log/warn "restore-parent-dirs" (ft/dirname path))

  (when-not (exists? cm (ft/dirname path))
    (mkdirs cm (ft/dirname path))
    (log/warn "Created " (ft/dirname path))

    (loop [parent (ft/dirname path)]
      (log/warn "restoring path" parent)
      (log/warn "user parent path" user)

      (when (and (not= parent (user-home-dir user)) (not (owns? cm user parent)))
        (log/warn (str "Restoring ownership of parent dir: " parent))
        (set-owner cm parent user)
        (recur (ft/dirname parent))))))

(defn- restore-path
  [{:keys [user paths user-trash]}]
  (with-jargon (jargon-cfg) [cm]
    (let [paths (mapv ft/rm-last-slash paths)]
      (validators/user-exists cm user)
      (validators/all-paths-exist cm paths)
      (validators/all-paths-writeable cm user paths)
      
      (let [retval (atom (hash-map))]
        (doseq [path paths]
          (let [fully-restored      (ft/rm-last-slash (restoration-path cm user path))
                restored-to-homedir (restore-to-homedir? cm path)]
            (log/warn "Restoring " path " to " fully-restored)
            
            (validators/path-not-exists cm fully-restored)
            (log/warn fully-restored " does not exist. That's good.")
            
            (restore-parent-dirs cm user fully-restored)
            (log/warn "Done restoring parent dirs for " fully-restored)

            (validators/path-writeable cm user (ft/dirname fully-restored))
            (log/warn fully-restored "is writeable. That's good.")

            (log/warn "Moving " path " to " fully-restored)
            (validators/path-not-exists cm fully-restored)

            (log/warn fully-restored " does not exist. That's good.")
            (move cm path fully-restored :user user :admin-users (irods-admins))
            (log/warn "Done moving " path " to " fully-restored)

            (reset! retval
                    (assoc @retval path {:restored-path fully-restored
                                         :partial-restore restored-to-homedir}))))
        {:restored @retval}))))

(defn- list-in-dir
  [cm fixed-path]
  (let [ffilter (proxy [java.io.FileFilter] [] (accept [stuff] true))]
    (.getListInDirWithFileFilter
      (:fileSystemAO cm)
      (file cm fixed-path)
      ffilter)))

(defn- delete-trash
  [user]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [trash-dir  (user-trash-path cm user)
          trash-list (mapv #(.getAbsolutePath %) (list-in-dir cm (ft/rm-last-slash trash-dir)))]
      (doseq [trash-path trash-list]
        (delete cm trash-path))
      {:trash trash-dir
       :paths trash-list})))

(defn do-delete
  [{user :user} {paths :paths}]  
  (delete-paths user paths))

(with-pre-hook! #'do-delete
  (fn [params body] 
    (log/warn "[call][do-delete]" params body)
    (validate-map params {:user string?})
    (validate-map body   {:paths sequential?})
    (validate-num-paths (:paths body))
    
    (when (super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))))

(with-post-hook! #'do-delete 
  (fn [result]
    (log/warn "[result][do-delete]" result)))

(defn do-restore
  [{user :user} {paths :paths}]
  (restore-path
    {:user  user
     :paths paths
     :user-trash (user-trash-path user)}))

(with-post-hook! #'do-restore 
  (fn [result]
    (log/warn "[result][do-restore]" result)))

(with-pre-hook! #'do-restore
  (fn [params body]
    (log/warn "[call][do-restore]" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})
    (validate-num-paths (:paths body))))

(defn do-user-trash
  [{user :user}]
  (user-trash user))

(with-pre-hook! #'do-user-trash
  (fn [params]
    (log/warn "[call][do-user-trash]" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-user-trash 
  (fn [result]
    (log/warn "[result][do-user-trash]" result)))

(defn do-delete-trash
  [{user :user}]
  (delete-trash user))

(with-post-hook! #'do-delete-trash 
  (fn [result]
    (log/warn "[result][do-delete-trash]" result)))

(with-pre-hook! #'do-delete-trash
  (fn [params]
    (log/warn "[call][do-delete-trash]" params)
    (validate-map params {:user string?})))

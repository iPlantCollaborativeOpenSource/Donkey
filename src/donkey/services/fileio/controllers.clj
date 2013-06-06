(ns donkey.services.fileio.controllers
  (:use [clj-jargon.jargon] 
        [clojure-commons.error-codes]
        [donkey.util.config]
        [donkey.util.validators]
        [donkey.util.transformers :only [add-current-user-to-map]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [donkey.services.fileio.actions :as actions]
            [donkey.services.fileio.controllers :as fileio]
            [cheshire.core :as json]
            [clojure-commons.file-utils :as ft]
            [clojure.string :as string]
            [donkey.util.ssl :as ssl]
            [clojure.tools.logging :as log]
            [ring.util.response :as rsp-utils]
            [cemerick.url :as url-parser]))

(defn in-stream
  [address]
  (try+
   (ssl/input-stream address)
   (catch java.net.UnknownHostException e
     (throw+ {:error_code ERR_INVALID_URL
              :url address}))
   (catch java.net.MalformedURLException e
     (throw+ {:error_code ERR_INVALID_URL
              :url address}))))

(defn gen-uuid []
  (str (java.util.UUID/randomUUID)))

(defn store
  [cm istream filename user dest-dir]
  (actions/store cm istream user (ft/path-join dest-dir filename)))

(defn store-irods
  [{stream :stream orig-filename :filename}]
  (let [uuid     (gen-uuid)
        filename (str orig-filename "." uuid)
        user     (irods-user)
        home     (irods-home)
        temp-dir (fileio-temp-dir)]
    (log/warn filename)
    (with-jargon (jargon-cfg) [cm]
      (store cm stream filename user temp-dir))))

(defn download
  [req-params]
  (let [params (add-current-user-to-map req-params)]
    (validate-map params {:user string? :path string?})
    (actions/download (:user params) (:path params))))

(defn upload
  [req-params req-multipart]
  (log/info "Detected params: " req-params)
  (validate-map req-params {"file" string? "user" string? "dest" string?})
  (let [user    (get req-params "user")
        dest    (get req-params "dest")
        up-path (get req-multipart "file")]
    (actions/upload user up-path dest)))

(defn url-filename
  [address]
  (let [parsed-url (url-parser/url address)]
    (when-not (:protocol parsed-url)
      (throw+ {:error_code ERR_INVALID_URL
                :url address}))

    (when-not (:host parsed-url)
      (throw+ {:error_code ERR_INVALID_URL
               :url address}))

    (if-not (string/blank? (:path parsed-url))
      (ft/basename (:path parsed-url))
      (:host parsed-url))))

(defn urlupload
  [req-params req-body]
  (let [params (add-current-user-to-map req-params)
        body   (parse-body (slurp req-body))]
    (validate-map params {:user string?})
    (validate-map body {:dest string? :address string?})
    (let [user    (:user params)
          dest    (string/trim (:dest body))
          addr    (string/trim (:address body))
          istream (in-stream addr)
          fname   (url-filename addr)]
      (log/warn (str "User: " user))
      (log/warn (str "Dest: " dest))
      (log/warn (str "Fname: " fname))
      (log/warn (str "Addr: " addr))
      (actions/urlimport user addr fname dest))))

(defn saveas
  [req-params req-body]
  (let [params (add-current-user-to-map req-params)
        body   (parse-body (slurp req-body))]
    (validate-map params {:user string?})
    (validate-map body {:dest string? :content string?})
    (let [user (:user params)
          dest (string/trim (:dest body))
          cont (:content body)]
      (with-jargon (jargon-cfg) [cm]
        (when-not (user-exists? cm user)
          (throw+ {:user       user
                   :error_code ERR_NOT_A_USER}))
        
        (when-not (exists? cm (ft/dirname dest))
          (throw+ {:error_code ERR_DOES_NOT_EXIST
                   :path       (ft/dirname dest)}))
        
        (when-not (is-writeable? cm user (ft/dirname dest))
          (throw+ {:error_code ERR_NOT_WRITEABLE
                   :path       (ft/dirname dest)}))
        
        (when (exists? cm dest)
          (throw+ {:error_code ERR_EXISTS 
                   :path       dest}))
        
        (with-in-str cont
          (actions/store cm *in* user dest)
          {:status "success"
           :file 
           {:id dest
            :label         (ft/basename dest)
            :permissions   (dataobject-perm-map cm user dest)
            :date-created  (created-date cm dest)
            :date-modified (lastmod-date cm dest)
            :file-size     (str (file-size cm dest))}})))))
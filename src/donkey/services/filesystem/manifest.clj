(ns donkey.services.filesystem.manifest
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
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.actions :as irods-actions]
            [donkey.services.filesystem.riak :as riak]
            [donkey.services.garnish.irods :as filetypes]
            [ring.util.codec :as cdc])
  (:import [org.apache.tika Tika]))

(defn- preview-url
  [user path]
  (str "file/preview?user=" (cdc/url-encode user) "&path=" (cdc/url-encode path)))

(defn- content-type
  [cm path]
  (.detect (Tika.) (input-stream cm path)))

(defn- extract-tree-urls
  [cm fpath]
  (if (attribute? cm fpath "tree-urls")
    (-> (get-attribute cm fpath "tree-urls")
        first
        :value
        riak/get-tree-urls
        (json/decode true)
        :tree-urls)
    []))

(defn- manifest
  [user path data-threshold]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-exists cm path)
      (validators/path-is-file cm path)
      (validators/path-readable cm user path)
      
      {:action       "manifest"
       :content-type (content-type cm path)
       :tree-urls    (extract-tree-urls cm path)
       :info-type    (filetypes/get-types cm user path)
       :mime-type    (.detect (Tika.) (input-stream cm path))
       :preview      (preview-url user path)})))

(defn do-manifest
  [{user :user path :path}]
  (manifest user path (fs-data-threshold)))

(with-pre-hook! #'do-manifest
  (fn [params]
    (log/warn "[call][do-manifest]" params)))

(with-post-hook! #'do-manifest (log-func "do-manifest"))

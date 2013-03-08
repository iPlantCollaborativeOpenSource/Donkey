(ns donkey.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-int listen-port
  "The port that donkey listens to."
  [props config-valid configs]
  "donkey.app.listen-port")

(cc/defprop-str metadactyl-base-url
  "The base URL to use when connecting to secured Metadactyl services."
  [props config-valid configs]
  "donkey.metadactyl.base-url")

(cc/defprop-str metadactyl-unprotected-base-url
  "The base URL to use when connecting to unsecured Metadactyl services."
  [props config-valid configs]
  "donkey.metadactyl.unprotected-base-url")

(cc/defprop-str notificationagent-base-url
  "The base URL to use when connecting to the notification agent."
  [props config-valid configs]
  "donkey.notificationagent.base-url")

(cc/defprop-str cas-server
  "The base URL used to connect to the CAS server."
  [props config-valid configs]
  "donkey.cas.cas-server")

(cc/defprop-str server-name
  "The name of the local server."
  [props config-valid configs]
  "donkey.cas.server-name")

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "donkey.uid.domain")

(cc/defprop-str riak-base-url
  "The base URL for the Riak HTTP API. Used for user sessions."
  [props config-valid configs]
  "donkey.sessions.base-url")

(cc/defprop-str riak-sessions-bucket
  "The bucket in Riak to retrieve user sessions from."
  [props config-valid configs]
  "donkey.sessions.bucket")

(cc/defprop-str riak-prefs-bucket
  "The bucket in Riak to retrieve user preferences from."
  [props config-valid configs]
  "donkey.preferences.bucket")

(cc/defprop-str riak-search-hist-bucket
  "The bucket in Riak to use for the storage of user search history."
  [props config-valid configs]
  "donkey.search-history.bucket")

(cc/defprop-str userinfo-base-url
  "The base URL for the user info API."
  [props config-valid configs]
  "donkey.userinfo.base-url")

(cc/defprop-str userinfo-key
  "The key to use when authenticating to the user info API."
  [props config-valid configs]
  "donkey.userinfo.client-key")

(cc/defprop-str userinfo-secret
  "The secret to use when authenticating to the user info API."
  [props config-valid configs]
  "donkey.userinfo.password")

(cc/defprop-str jex-base-url
  "The base URL for the JEX."
  [props config-valid configs]
  "donkey.jex.base-url")

(cc/defprop-int default-user-search-result-limit
  "The default limit for the number of results for a user info search.  Note
   this is the maximum number of results returned by trellis for any given
   search.  Our aggregate search may return the limit times the number of
   search types."
  [props config-valid configs]
  "donkey.userinfo.default-search-limit")

(cc/defprop-str nibblonian-base-url
  "The base URL for the Nibblonian data management services."
  [props config-valid configs]
  "donkey.nibblonian.base-url")

(cc/defprop-str scruffian-base-url
  "The base URL for the Scruffian file export and import services."
  [props config-valid configs]
  "donkey.scruffian.base-url")

(cc/defprop-str tree-parser-url
  "The URL for the tree parser service."
  [props config-valid configs]
  "donkey.tree-viewer.base-url")

(cc/defprop-str tree-url-bucket
  "The bucket in Riak to use for the storage of tree viewer URLs."
  [props config-valid configs]
  "donkey.tree-viewer.bucket")

(cc/defprop-str es-url
  "The URL for Elastic Search"
  [props config-valid configs]
  "donkey.infosquito.es-url")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  []
  (cc/load-config-from-file (System/getenv "IPLANT_CONF_DIR") "donkey.properties" props)
  (cc/log-config props)
  (validate-config))

(defn load-config-from-zookeeper
  "Loads the configuration settings from Zookeeper."
  []
  (cc/load-config-from-zookeeper props "donkey")
  (cc/log-config props)
  (validate-config))

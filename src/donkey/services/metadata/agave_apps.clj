(ns donkey.services.metadata.agave-apps
  (:use [donkey.auth.user-attributes :only [current-user]]
        [donkey.util.validators :only [validate-map]]
        [slingshot.slingshot :only [try+]])
  (:require [cemerick.url :as curl]
            [clojure.string :as string]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.common-apps :as ca]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(def ^:private agave-job-validation-map
  "The validation map to use for Agave jobs."
  {:name          string?
   :analysis_name string?
   :id            string?
   :startdate     string?
   :status        string?})

(defn- store-agave-job
  [agave id job]
  (validate-map job agave-job-validation-map)
  (jp/save-job (:id job) (:name job) jp/agave-job-type (:username current-user) (:status job)
               :id         id
               :app-name   (:analysis_name job)
               :start-date (db/timestamp-from-str (str (:startdate job)))
               :end-date   (db/timestamp-from-str (str (:enddate job)))))

(defn submit-agave-job
  [agave-client submission]
  (let [id     (UUID/randomUUID)
        cb-url (str (curl/url (config/agave-callback-base) (str id)))
        job    (.submitJob agave-client (assoc-in submission [:config :callbackUrl] cb-url))]
    (store-agave-job agave-client id job)
    (dn/send-agave-job-status-update (:shortUsername current-user) job)))

(defn format-agave-job
  [job state]
  (assoc state
    :id            (:id job)
    :startdate     (str (or (db/millis-from-timestamp (:startdate job)) 0))
    :enddate       (str (or (db/millis-from-timestamp (:enddate job)) 0))
    :analysis_name (:analysis_name job)
    :status        (:status job)))

(defn load-agave-job-states
  [agave jobs]
  (let [agave-jobs (filter (comp ca/agave-job-id? :id) jobs)]
    (if-not (empty? agave-jobs)
      (->> (.listJobs agave (map :id agave-jobs))
           (map (juxt :id identity))
           (into {}))
      {})))

(defn get-agave-job
  [agave id not-found-fn]
  (try+
   (not-empty (.listRawJob agave id))
   (catch [:status 404] _ (not-found-fn id))
   (catch [:status 400] _ (not-found-fn id))
   (catch Object _ (service/request-failure "lookup for HPC job" id))))

(defn update-agave-job-status
  [agave id username prev-status]
  (let [job-info (get-agave-job agave id (partial service/not-found "HPC job"))]
    (service/assert-found job-info "HPC job" id)
    (when-not (= (:status job-info) prev-status)
      (jp/update-job id (:status job-info) (db/timestamp-from-str (str (:enddate job-info))))
      (dn/send-agave-job-status-update username job-info))))

(defn remove-deleted-agave-jobs
  "Marks jobs that have been deleted in Agave as deleted in the DE also."
  [agave]
  (let [extant-jobs (set (.listJobIds agave))]
    (->> (jp/get-external-job-ids (:username current-user) {:job-types [jp/agave-job-type]})
         (remove extant-jobs)
         (map #(jp/update-job % {:deleted true}))
         (dorun))))

(defn- agave-job-status-changed
  [job curr-state]
  (or (nil? curr-state)
      (not= (:status job) (:status curr-state))
      ((complement string/blank?) (:enddate curr-state))))

(defn sync-agave-job-status
  [agave job]
  (let [curr-state (get-agave-job agave (:external_id job) (constantly nil))]
    (when (agave-job-status-changed job curr-state)
      (jp/update-job-by-internal-id
       (:id job)
       {:status   (:status curr-state)
        :end-date (db/timestamp-from-str (str (:enddate curr-state)))
        :deleted  (nil? curr-state)}))))

(defn get-agave-app-rerun-info
  [agave job-id]
  (.getAppRerunInfo agave job-id))

(defn get-agave-job-params
  [agave job-id]
  (.getJobParams agave job-id))

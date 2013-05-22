(ns donkey.services.garnish.controllers
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [donkey.util.validators]
        [donkey.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as json]
            [hoot.rdf :as rdf]
            [hoot.csv :as csv]
            [clojure.core.memoize :as memo]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.services.garnish.irods :as prods]
            [donkey.util.config :as cfg]))

(def script-types 
  ["ace"
   "blast"
   "bowtie"
   "clustalw"
   "codata"
   "csv"
   "embl"
   "fasta"
   "fastq"
   "fastxy"
   "game"
   "gcg"
   "gcgblast"
   "gcgfasta"
   "gde"
   "genbank"
   "genscan"
   "gff"
   "hmmer"
   "nexus"
   "mase"
   "mega"
   "msf"
   "phrap"
   "pir"
   "pfam"
   "phylip"
   "prodom"
   "raw"
   "rsf"
   "selex"
   "stockholm"
   "swiss"
   "tab"
   "vcf"])

(defn accepted-types
  []
  (set (concat rdf/accepted-languages csv/csv-types)))

(defn add-type
  [req-body req-params]
  (let [body   (parse-body (slurp req-body))
        params (add-current-user-to-map req-params)]
    (validate-map params {:user string?})
    (validate-map body {:path string? 
                           :type #(contains? (accepted-types) %)})
    (json/generate-string
      (prods/add-type (:user params) (:path body) (:type body)))))

(defn delete-type
  [req-params]
  (let [params (add-current-user-to-map req-params)] 
    (validate-map params {:user string? 
                             :type #(contains? (accepted-types) %) 
                             :path string?})
    (json/generate-string
      (prods/delete-type (:user params) (:path params) (:type params)))))

(defn get-types
  [req-params]
  (let [params (add-current-user-to-map req-params)] 
    (validate-map params {:path string? 
                             :user string?})
    (json/generate-string
      {:types (prods/get-types (:user params) (:path params))})))

(defn find-typed-paths
  [req-params]
  (let [params (add-current-user-to-map req-params)] 
    (validate-map params {:user string? :type string?})
    (json/generate-string
      {:paths (prods/find-paths-with-type (:user params) (:type params))})))

(defn get-type-list 
  [] 
  (json/generate-string {:types (seq (set (concat csv/csv-types script-types)))}))

(defn set-auto-type
  [req-body req-params]
  (let [body   (parse-body (slurp req-body))
        params (add-current-user-to-map req-params)]
    (log/warn body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (json/generate-string
      (prods/auto-add-type (:user params) (:path body)))))

(defn preview-auto-type
  [req-params]
  (let [params (add-current-user-to-map req-params)] 
    (validate-map params {:user string? :path string?})
    (json/generate-string
      (prods/preview-auto-type (:user params) (:path params)))))

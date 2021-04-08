(ns cmis-clj.core
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.data.zip.xml :refer [attr attr= xml1->]]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [sanitize-filename.core :as sanitize])
  (:import java.util.HashMap
           org.apache.chemistry.opencmis.client.SessionFactoryFinder
           org.apache.chemistry.opencmis.commons.enums.BindingType
           org.apache.chemistry.opencmis.commons.SessionParameter))

(def default-url "http://pam01.sbszh.ch/alfresco/cmisatom")
(def default-repo-id "884e10c7-1b45-41ab-b9ff-58e9fb84d688")
(def default-query "SELECT * FROM sbs:daisyFile WHERE sbs:pKontrolleEB LIKE '20%' AND CONTAINS('PATH:\"/app:company_home/cm:Produktion/cm:Archiv/cm:Bücher//*\"')")

(defn to-map [result]
  (->>
   (.getProperties result)
   (map (fn [p] [(.getQueryName p) (.getFirstValue p)]))
   (into {})))

(defn get-document [session query-result]
  (some->>
   (.getPropertyValueById query-result "cmis:objectId")
   (.getObject session)))

(defn property [document name]
  (some-> document (.getProperty name) .getValue))

(defn ebook? [document]
  (let [obj-type (property document "cmis:objectTypeId")
        product-no (property document "sbs:pProduktNo")]
    (and
     obj-type (= obj-type "F:sbs:produkt")
     product-no (string/starts-with? product-no "EB"))))

(defn daisy-file? [document]
  (let [obj-type (property document "cmis:objectTypeId")]
    (= obj-type "D:sbs:daisyFile")))

(defn ebook-number [document]
  (let [parent (first (.getParents document))
        children (.getChildren parent)
        ebook (first (filter ebook? children))]
    (some-> ebook (property "sbs:pProduktNo"))))

(defn- kontrolle-to-iso8601 [s]
  (if-let [date (re-find #"\d{2}\.\d{2}\.\d{4}" s)]
    (let [input-format (java.text.SimpleDateFormat. "dd.MM.yyyy")
          iso-format (java.text.SimpleDateFormat. "yyyy-MM-dd")]
      (.format iso-format (.parse input-format s)))
    s))

(defn ebook-review-date [document]
  (let [parent (first (.getParents document))
        children (.getChildren parent)
        daisy-xml (first (filter daisy-file? children))]
    (some-> daisy-xml (property "sbs:pKontrolleEB") kontrolle-to-iso8601)))

(defn- to-iso8601 [date]
  (.format (java.text.SimpleDateFormat. "yyy-MM-dd") date))

(defn ebook-last-modification-date [document]
  (let [parent (first (.getParents document))
        children (.getChildren parent)
        ebook (first (filter ebook? children))]
    (some-> ebook (property "cmis:lastModificationDate") .getTime to-iso8601)))

(defn isbn [document]
  (let [parent (first (.getParents document))]
    (some-> parent (property "sbs:pISBN"))))

(defn dc-identifier [content]
  (let [root (-> content xml/parse zip/xml-zip)]
    (xml1-> root :dtbook :head :meta (attr= :name "dc:Identifier") (attr :content))))

(defn extract-ebook-numbers
  ([user password]
   (extract-ebook-numbers user password default-query default-url default-repo-id))
  ([user password query url repo-id]
   (let [factory (SessionFactoryFinder/find)
         params (HashMap. {SessionParameter/USER user
                           SessionParameter/PASSWORD password
                           SessionParameter/ATOMPUB_URL url
                           SessionParameter/BINDING_TYPE (.value (BindingType/ATOMPUB))
                           SessionParameter/REPOSITORY_ID repo-id})
         session (.createSession factory params)
         results (.query session query false)]
     (for [result results]
       (let [doc (get-document session result)
             ebook (ebook-number doc)
             isbn (isbn doc)
             id (.getId doc)
             dc-identifier (dc-identifier (some-> doc .getContentStream .getStream))
             review-date (ebook-review-date doc)
             last-modification-date (ebook-last-modification-date doc)]
         [ebook isbn dc-identifier id review-date last-modification-date])))))

(defn write-ebook-numbers [writer data]
  (csv/write-csv writer data))

(defn extract-content
  ([user password]
   (extract-content user password default-query default-url default-repo-id))
  ([user password query url repo-id]
   (let [factory (SessionFactoryFinder/find)
         params (HashMap. {SessionParameter/USER user
                           SessionParameter/PASSWORD password
                           SessionParameter/ATOMPUB_URL url
                           SessionParameter/BINDING_TYPE (.value (BindingType/ATOMPUB))
                           SessionParameter/REPOSITORY_ID repo-id})
         session (.createSession factory params)
         results (.query session query false)]
     (doseq [result results]
       (let [doc (get-document session result)
             ebook (ebook-number doc)
             id (.getId doc)
             content (some-> doc .getContentStream .getStream slurp)
             filename (format "%s.xml" (or ebook id))
             sanitized-filename (sanitize/sanitize filename)]
         (println (format"Exporting %s to export/%s..." id sanitized-filename))
         (spit (format "export/%s" sanitized-filename) content))))))


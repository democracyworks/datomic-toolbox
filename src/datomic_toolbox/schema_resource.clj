(ns datomic-toolbox.schema-resource
  (:require [clojure.java.io :as io]))

(defprotocol INamedResource
  (resource-name [resource]))
(extend-type java.net.URL
  INamedResource
  (resource-name [url]
    (-> url
        str
        (clojure.string/split #"/(?=.)")
        last)))
(extend-type java.io.File
  INamedResource
  (resource-name [file]
    (.getName file)))
(extend-type java.lang.String
  INamedResource
  (resource-name [str] str))

(defn jarred [resource directory]
  (->> resource
       .getPath
       (re-find #"^[^:]*:(.*)!")
       second
       java.util.jar.JarFile.
       .entries
       enumeration-seq
       (filter #(.startsWith (str %) (str directory "/")))
       (map (comp io/resource str))))

(defn vfs [resource]
  (->> resource
       .getContent
       .getChildren
       (map #(.getPhysicalFile %))))

(defn files [directory]
  (let [resource (io/resource directory)
        files    (condp = (.getProtocol resource)
                   "jar" (jarred resource directory)
                   "vfs" (vfs resource)
                   (-> resource io/as-file file-seq))]
    (->> files
         (filter #(.endsWith (resource-name %) ".edn"))
         (sort-by #(resource-name %)))))

(defn file->tx-data [file]
  (->> file slurp (clojure.edn/read-string {:readers *data-readers*})))

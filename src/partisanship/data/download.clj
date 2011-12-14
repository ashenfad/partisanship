(ns partisanship.data.download
  (:require (clojure.java [io :as io])
            (partisanship.data [vote :as vote])))

(defn -main [& args]
  (println "Downloading data to 'data/'...")
  (doseq [file (vals vote/files)]
    (io/copy (io/input-stream (str vote/external-loc file))
             (io/output-stream (str vote/data-dir file))))
  (println "Finished!"))

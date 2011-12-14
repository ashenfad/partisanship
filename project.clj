(defproject partisanship "1.0.0-SNAPSHOT"
  :description "Partisanship metrics"
  :run-aliases {:scrape-house partisanship.data.scrape-house
                :scrape-senate partisanship.data.scrape-senate
                :graphs partisanship.graph}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.csv "0.1.0"]
                 [org.clojure/data.json "0.1.2"]
                 [incanter "1.3.0-SNAPSHOT"]
                 [clj-time "0.3.3"]
                 [enlive "1.0.0"]])

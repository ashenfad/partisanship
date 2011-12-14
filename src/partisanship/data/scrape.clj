(ns partisanship.data.scrape
  (:require (partisanship.data [vote :as vote]
                               [scrape-senate :as senate]
                               [scrape-house :as house])))

(defn -main [& args]
  (println "Updating roll-call votes...")
  (doall (pmap #(apply vote/update %)
               [[senate/scraper :senate] [house/scraper :house]]))
  (println "Finished!"))

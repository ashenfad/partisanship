(ns partisanship.data.scrape-house
  (:require (partisanship.data [vote :as vote])
            (clj-time [format :as tf])
            (net.cgrand [enlive-html :as html])))

(def in-fmt (tf/formatter "dd-MMM-yyyy"))

(defn- to-url [year vote]
  (str "http://clerk.house.gov/evs/" year "/roll" (format "%03d" vote) ".xml"))

(defn pattern [res pattern]
  (map html/text (html/select res pattern)))

(defn- scraper [year vote]
  (let [res (html/html-resource (java.net.URL. (to-url year vote)))
        parties (pattern res [:party])
        yeas (butlast (pattern res [:yea-total]))
        nays (butlast (pattern res [:nay-total]))
        date (first (pattern res [:action-date]))
        outcome (apply merge (map (fn [party yea nay]
                                    {(keyword (str (first party)))
                                     {:yea (Integer/parseInt yea)
                                      :nay (Integer/parseInt nay)}})
                                  parties yeas nays))]
    (when date
      (println "  Scraping" year vote)
      (if (not (empty? outcome))
        {:year year
         :vote vote
         :date (tf/unparse vote/date-fmt (tf/parse in-fmt date))
         :outcome outcome}
        {}))))

(defn -main [& args]
  (println "Update house roll-call votes.  This might take a while...")
  (vote/update scraper :house)
  (println "Finished!"))

(ns nfl-parser.core
  (:require [net.cgrand.enlive-html :as html])
  (:gen-class))

(def base-url "http://www.nfl.com")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn scrape-team-links []
   (html/select (fetch-url (str base-url "/teams"))
                [(html/attr-starts :href "/team")]))

(defn get-roster-href [col]
  (clojure.string/replace (str base-url
                (get-in (into {} col) [:attrs :href]))
                #"profile" "roster"))

(defn get-links []
  (into #{}
        (map get-roster-href (scrape-team-links))))

(defn scrape-roster [url]
  (map html/text
       (html/select (fetch-url url)
                    [:div#team-stats-wrapper :table#result :tbody :tr])))

(defn scrape-all-rosters []
   (pmap scrape-roster (get-links)))
  ; (into '() (r/map scrape-roster (get-links))))

(defn format-player [player-data]
  (str (clojure.string/replace player-data #"\n" "\t") "\n"))

(defn get-player [team-data]
  (map format-player team-data))

(defn get-team [all-teams-data]
  (map get-player all-teams-data))

(defn -main []
  (time (doall
          (get-team (scrape-all-rosters)))))

; format returned by main
; (
;  ("team1-player1" "team1-player2" ...)
;  ("team2-player1" "team2-player2" ...)
;  ...
; )

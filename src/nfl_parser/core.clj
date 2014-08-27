(ns nfl-parser.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.string :as string])
  (:gen-class))

(def base-url "http://www.nfl.com")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn scrape-team-links []
   (html/select (fetch-url (str base-url "/teams"))
                [(html/attr-starts :href "/team")]))

(defn get-roster-href [col]
  (string/replace (str base-url
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

(defn player-string-to-map
  "Convert player to map"
  [player-data]
  (player-vector-to-map (player-string-to-vector player-data)))

(defn player-vector-to-map
  "Convert player vector to map with playername as key"
  [[number name & rest :as player-data]]
  {(player-name-to-key name) (zipmap [:number :name :position :status :height :weight :birth :experience :college] player-data)})

(defn player-name-to-key
  "Convert player name to key (:first-last)"
  [name]
  (keyword (string/replace
             (string/trim name)
             #", "
             "-")))

(defn player-string-to-vector
  "Convert player string to vector"
  [player-str]
  (string/split
    (string/trim player-str)
    #"\n"))

(defn get-player [team-data]
  (into {} (map player-string-to-map team-data)))

(defn get-team [all-teams-data]
  (into {} (map get-player all-teams-data)))

(def players
  (get-team (scrape-all-rosters)))

(defn get-player-data [lookup-key]
  (get-in players lookup-key))

(defn -main [lookup-keys]
  (if (vector? (first lookup-keys))
    (map get-player-data lookup-keys)
    (get-player-data lookup-keys)))

; example usage
; single player/item lookup
; (-main [:Manning-Peyton])
; (-main [:Manning-Peyton :college])
; (-main [:Manning-Peyton :status])
; multi player/item lookup
; (-main [[:Romo-Tony :height] [:Manning-Peyton :height]])
; (-main [[:Romo-Tony :height] [:Manning-Peyton :height] [:Brees-Drew :height]])

; format returned by players
; {
;  {:player1 {:data1 x :data2 y}} {:player2 {:data1 x :data2 y}}
;  {:player1 {:data1 x :data2 y}} {:player2 {:data1 x :data2 y}}
;  ...
; }

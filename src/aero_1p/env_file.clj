(ns aero-1p.env-file
  (:require [clojure.string :as str]))

(defn ^:private unquote-value [v]
  (if (and (>= (count v) 2)
           (#{"\"" "'"} (subs v 0 1))
           (= (subs v 0 1) (subs v (dec (count v)))))
    (subs v 1 (dec (count v)))
    v))

(defn parse
  "Parses dotenv-style content into a map of var name -> value.
   Supports blank lines, `#` comments, optional `export` prefix and single/double
   quoted values. No variable expansion, no multi-line values, no inline comments
   (`#` is kept as part of the value)."
  [content]
  (->> (str/split-lines content)
       (map str/trim)
       (remove #(or (str/blank? %) (str/starts-with? % "#")))
       (keep (fn [line]
               (let [line (str/replace line #"^export\s+" "")
                     [k v] (str/split line #"=" 2)]
                 (when v
                   [(str/trim k) (unquote-value (str/trim v))]))))
       (into {})))

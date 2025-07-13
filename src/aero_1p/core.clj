(ns aero-1p.core
  (:require [aero.core :as aero]
            [clojure.java.process :as proc]
            [clojure.string :as str]))

(defn op-read-cmd
  [{:keys [account path]}]

  (->> ["op" "read"
        (when account ["--account" account])
        path]
       flatten
       (remove nil?)))

(defn run [cmd]

  (-> (apply proc/start cmd)
      proc/stdout
      slurp
      str/trim))

(defmethod aero/reader 'op/secret
  [_opts _tag value]
  (let [{:keys [account path]} (cond
                                 (string? value) {:account nil
                                                  :path value}
                                 (map? value) value)
        cmd (op-read-cmd {:account account :path path})]
    (run cmd)))

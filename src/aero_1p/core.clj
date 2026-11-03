(ns aero-1p.core
  "Aero reader tags for 1Password.
   `op` client can be swapped by passing `:aero-1p/op-cli` (fn of account -> `aero-1p.op/OpCli`)
   in `aero/read-config` opts."
  (:require [aero-1p.env-file :as env-file]
            [aero-1p.op :as op]
            [aero.core :as aero]
            [clojure.java.io :as io])
  (:import [java.util.concurrent ConcurrentHashMap]))

(defn ^:private op-cli [opts account]
  ((get opts :aero-1p/op-cli op/cli) account))

(defmethod aero/reader 'op/secret
  [opts _tag value]
  (let [{:keys [account path]} (if (string? value) {:path value} value)
        client (op-cli opts account)]
    (aero/->Deferred (future (op/read client path)))))

;;; env file support

;; [client resource-url] -> delay of parsed env map
;; ensures `op inject` runs once per file+account, no matter how many config
;; values reference it
(defonce ^:private env-file-cache
  (ConcurrentHashMap.))

(defn clear-cache!
  "Drops cached results of `op inject`, next config read will invoke `op` again."
  []
  (ConcurrentHashMap/.clear env-file-cache))

(defn ^:private load-env-file
  "Returns parsed env map for given resource, running `op inject` at most once
   per [client url]. Failures are not cached."
  [client url]
  (let [k [client (str url)]
        cached (ConcurrentHashMap/.computeIfAbsent
                env-file-cache k
                (fn [_] (delay (env-file/parse (op/inject client (slurp url))))))]
    (try
      @cached
      (catch Exception e
        ;; only evicts if no one replaced the entry in the meantime
        (ConcurrentHashMap/.remove env-file-cache k cached)
        (throw e)))))

(defmethod aero/reader 'op/env
  [opts _tag {:keys [file account] var-name :var}]
  (if-let [url (and file (io/resource file))]
    (let [client (op-cli opts account)]
      (aero/->Deferred (future (get (load-env-file client url) var-name))))
    (System/getenv var-name)))

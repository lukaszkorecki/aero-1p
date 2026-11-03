(ns aero-1p.core-test
  (:require
   [aero-1p.core :as aero-1p]
   [aero-1p.op :as op]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defrecord FakeOp [account calls]
  op/OpCli
  (read [_ path]
    (Thread/sleep 50)
    (swap! calls conj [:read account path])
    {:account account :path path})
  (inject [_ template]
    (Thread/sleep 50)
    (swap! calls conj [:inject account])
    (str/replace template #"op://" "injected:")))

(defn ^:private read-config [resource calls]
  (aero/read-config (io/resource resource)
                    {:aero-1p/op-cli #(->FakeOp % calls)}))

(deftest loading-test
  (testing "Reads each secret reference with the configured account"
    (is (= {:op-acc-id "mycompany.1password.com"
            :github {:org "test"
                     :pat {:account "mycompany.1password.com"
                           :path "op://Private/github/test"}
                     :repo {:account "mycompany.1password.com"
                            :path "op://Private/github/test/repo"}}
            :aws {:access-key-id {:account "mycompany.1password.com"
                                  :path "op://Employee/aws/test/access-key-id"}
                  :secret-access-key {:account "mycompany.1password.com"
                                      :path "op://Employee/aws/test/secrets-access-key"}}
            :personal {:token {:account nil
                               :path "op://Private/personal/token"}
                       :email {:account "my.1password.com"
                               :path "op://Private/personal/email"}}}
           (read-config "example.edn" (atom []))))))

(deftest env-file-loading-test
  (testing "Runs op inject once per file+account and reads vars from the result"
    (aero-1p/clear-cache!)
    (let [calls (atom [])]
      (is (= {:op-acc-id "mycompany.1password.com"
              :db {:password "injected:Employee/db/password"}
              :api {:token "injected:Employee/api/token"
                    :single "injected:Employee/single/value"}
              :personal {:token "injected:Employee/api/token"}
              :missing-file (System/getenv "HOME")}
             (read-config "example_env.edn" calls)))
      (is (= #{[:inject "mycompany.1password.com"] [:inject nil]}
             (set @calls)))
      (is (= 2 (count @calls))))))

(defrecord FailingOp [account calls]
  op/OpCli
  (read [_ _path])
  (inject [_ _template]
    (swap! calls conj [:inject account])
    (throw (ex-info "op failed" {}))))

(deftest env-file-failure-test
  (testing "Failed op inject is not cached"
    (aero-1p/clear-cache!)
    (let [calls (atom [])
          read! #(aero/read-config (io/resource "example_env.edn")
                                   {:aero-1p/op-cli (fn [account] (->FailingOp account calls))})]
      (is (thrown? Exception (read!)))
      (let [n (count @calls)]
        (is (thrown? Exception (read!)))
        (is (< n (count @calls)))))))

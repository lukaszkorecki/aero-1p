(ns aero-1p.core-test
  (:require
   [aero-1p.core :as aero-1p]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

;;; NOTE: we can't really test again real op CLI but this gives us a good approximation of how the config map will be processed

(deftest loading-test
  (with-redefs [aero-1p/run! (fn [cmd]
                               (Thread/sleep 50)
                               {:cmd cmd})]

    (let [expected {:op-acc-id "mycompany.1password.com"
                    :github {:org "test"
                             ;; read from default account e.g. the personal one
                             :pat {:cmd ["op" "read"
                                         "--account" "mycompany.1password.com"
                                         "op://Private/github/test"]}

                             :repo {:cmd ["op" "read"
                                          "--account" "mycompany.1password.com"
                                          "op://Private/github/test/repo"]}}

                    :aws {:access-key-id {:cmd ["op" "read"
                                                "--account" "mycompany.1password.com"
                                                "op://Employee/aws/test/access-key-id"]}
                          :secret-access-key {:cmd ["op" "read"
                                                    "--account" "mycompany.1password.com"
                                                    "op://Employee/aws/test/secrets-access-key"]}}

                    :personal {:token {:cmd ["op" "read"
                                             "op://Private/personal/token"]}
                               :email {:cmd ["op" "read"
                                             "--account" "my.1password.com"
                                             "op://Private/personal/email"]}}}
          loaded-config (aero/read-config (io/resource "example.edn"))]

      (is (= expected loaded-config)))))

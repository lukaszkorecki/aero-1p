(ns aero-1p.op-test
  (:require
   [aero-1p.op :as op]
   [clojure.test :refer [deftest is testing]]))

(deftest cmd-test
  (testing "Adds --account only when account is set"
    (is (= ["op" "read" "op://a/b/c"]
           (#'op/cmd nil "read" "op://a/b/c")))
    (is (= ["op" "inject" "--account" "acme.1password.com"]
           (#'op/cmd "acme.1password.com" "inject")))))

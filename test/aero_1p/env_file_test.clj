(ns aero-1p.env-file-test
  (:require
   [aero-1p.env-file :as env-file]
   [clojure.test :refer [deftest is testing]]))

(deftest parse-test
  (testing "Parses dotenv-style content"
    (is (= {"A" "1" "B" "two words" "C" "x=y" "D" "single" "E" ""}
           (env-file/parse (str "# comment\n"
                                   "\n"
                                   "export A=1\n"
                                   "B=\"two words\"\n"
                                   "  C=x=y\n"
                                   "D='single'\n"
                                   "E=\n"
                                   "garbage\n"))))))

(ns scalajars.test.unit.web.jar
  (:require [scalajars.web.jar :as jar]
            [clojure.test :refer :all]
            [scalajars.test.test-helper :as help]))

(use-fixtures :each help/default-fixture)

(deftest bad-homepage-url-shows-as-text
  (with-out-str
    (let [html (jar/show-jar nil {:homepage "something thats not a url"
                                  :created 3
                                  :version "1"
                                  :group_name "test"
                                  :jar_name "test"} [] 0)]
      (is (re-find #"something thats not a url" html)))))

(deftest pages-are-escaped
  (with-out-str
    (let [html (jar/show-jar nil {:homepage nil
                                  :created 3
                                  :version "<script>alert('hi')</script>"
                                  :group_name "test"
                                  :jar_name "test"} [] 0)]
      (is (not (.contains html "<script>alert('hi')</script>"))))))

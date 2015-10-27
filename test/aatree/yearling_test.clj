(ns aatree.yearling-test
  (:require [clojure.test :refer :all]
            [aatree.core :refer :all]
            [aatree.yearling :refer :all])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(deftest yearling
  (.delete (File. "yearling-test.yearling"))

  (let [opts (yearling-open (File. "yearling-test.yearling") 10000 100000)
        _ (is (= (db-transaction-count opts) 2))
        aamap (db-get-sorted-map opts)
        _ (is (= aamap {}))
        _ (db-update (fn [aamap opts]
                       (assoc aamap :block (db-allocate opts)))
                     opts)
        aamap (db-get-sorted-map opts)
        _ (is (= aamap {:block 20000}))
        _ (is (= (db-transaction-count opts) 3))
        _ (is (= (db-allocated opts) 3))
        _ (is (= (count (db-release-pending opts)) 0))
        _ (db-update (fn [aamap opts]
                       (db-release (:block aamap) opts)
                       (dissoc aamap :block))
                     opts)
        _ (is (= (db-transaction-count opts) 4))
        _ (db-close opts)])

  (let [opts (yearling-open (File. "yearling-test.yearling") 10000 100000)
        _ (is (= (db-transaction-count opts) 4))
        aamap (db-get-sorted-map opts)
        _ (is (= aamap {}))
        _ (is (= (db-allocated opts) 3))
        _ (is (= (count (db-release-pending opts)) 1))
        _ (db-close opts)])

  (Thread/sleep 200))

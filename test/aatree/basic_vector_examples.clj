(ns aatree.basic-vector-examples
  (:require [aatree.core :refer :all]))

(def bv1 (conj emptyAAVector 1 2 3))
(println bv1); -> [1 2 3]

(def bv2 (addn bv1 0 0))
(println bv2); -> [0 1 2 3]

(def bv3 (addn bv2 3 20))
(println bv3); -> [0 1 2 20 3]

(def bv4 (dropn bv3 1))
(println bv4); -> [0 2 20 3]

(def s1 (seq bv4))
(println s1); -> (0 2 20 3)
(println (count s1)); -> 4

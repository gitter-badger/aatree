(ns aatree.lazy-sorted-set-examples
  (:require [aatree.core :refer :all])
  (:import (java.nio ByteBuffer)))

(set! *warn-on-reflection* true)

(def opts (lazy-opts))

(def empty-set (new-sorted-set opts))
(println (byte-length empty-set)); -> 1

(def ls1 (conj empty-set :dog :cat :rabbit))
(println ls1); -> #{:cat :dog :rabbit}

(def ls1-len (byte-length ls1))
(println ls1-len); -> 85

(def ^ByteBuffer bb (ByteBuffer/allocate ls1-len))
(put-bytebuffer ls1 bb)
(.flip bb)
(def ls2 (load-lazy-set bb))
(println ls2); -> #{:cat :dog :rabbit}

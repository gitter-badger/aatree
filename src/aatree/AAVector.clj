(ns aatree.AAVector
  (:gen-class
    :main false
    :extends clojure.lang.APersistentVector
    :implements [clojure.lang.IObj
                 aatree.nodes.FlexVector
                 aatree.nodes.INoded]
    :constructors {[aatree.nodes.INode clojure.lang.IPersistentMap]
                   []
                   [aatree.nodes.INode clojure.lang.IPersistentMap clojure.lang.IPersistentMap]
                   []}
    :init init
    :state state)
  (:require [aatree.nodes :refer :all])
  (:import (aatree AAVector)
           (aatree.nodes INode)
           (clojure.lang IPersistentMap)))

(set! *warn-on-reflection* true)

(defn -getState [^AAVector this]
  (.-state this))

(defn ^IPersistentMap -getOpts [this]
  (.opts (get-state this)))

(defn- ^IPersistentMap get-state-meta [this]
  (.-meta (get-state this)))

(defn -init
  ([node opts]
   [[] (->noded-state node opts nil)])
  ([node opts meta]
   [[] (->noded-state node opts meta)]))

(defn -meta [^AAVector this] (get-state-meta this))

(defn -withMeta [^AAVector this meta] (new AAVector (get-inode this) (-getOpts this) meta))

(defn -count [this]
  (.getCnt (get-inode this) (-getOpts this)))

(defn -nth
  ([^AAVector this i]
   (nth-t2 (get-inode this) i (-getOpts this)))
  ([this i notFound]
   (if (and (>= i 0) (< i (-count this)))
     (-nth this i)
     notFound)))

(defn -cons [^AAVector this val]
  (let [n0 (get-inode this)
        n1 (vector-add n0 val (-count this) (-getOpts this))]
    (new AAVector n1 (-getOpts this) (get-state-meta this))))

(defn -addNode [^AAVector this i val]
  (let [c (-count this)]
    (cond
      (= i c)
      (-cons this val)
      (and (>= i 0) (< i c))
      (let [n0 (get-inode this)
            n1 (vector-add n0 val i (-getOpts this))]
        (new AAVector n1 (-getOpts this) (get-state-meta this)))
      :else
      (throw (IndexOutOfBoundsException.)))))

(defn -assocN [^AAVector this i val]
  (let [c (-count this)]
    (cond
      (= i c)
      (-cons this val)
      (and (>= i 0) (< i c))
      (let [n0 (get-inode this)
            n1 (vector-set n0 val i (-getOpts this))]
        (new AAVector n1 (-getOpts this) (get-state-meta this)))
      :else
      (throw (IndexOutOfBoundsException.)))))

(defn -empty [^AAVector this]
  (new AAVector
       (empty-node (get-inode this) (-getOpts this))
       (-getOpts this)
       (get-state-meta this)))

(defn -iterator [^AAVector this]
  (new-counted-iterator (get-inode this) (-getOpts this)))

(defn -seq
  [^AAVector this]
  (new-counted-seq (get-inode this) (-getOpts this)))

(defn -pop [^AAVector this]
  (if (empty? this)
    this
    (let [n0 (get-inode this)
          n1 (deln n0 (- (-count this) 1) (-getOpts this))]
      (new AAVector n1 (-getOpts this) (get-state-meta this)))))

(defn -dropNode [^AAVector this i]
  (if (or (< i 0) (>= i (-count this)))
    this
    (new AAVector
         (deln (get-inode this) i (-getOpts this))
         (-getOpts this)
         (get-state-meta this))))
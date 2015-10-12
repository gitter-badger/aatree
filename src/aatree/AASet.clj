(ns aatree.AASet
  (:gen-class
    :main false
    :extends clojure.lang.APersistentSet
    :implements [clojure.lang.IObj
                 clojure.lang.Reversible
                 clojure.lang.Sorted
                 clojure.lang.Counted
                 clojure.lang.Indexed
                 aatree.nodes.INoded]
    :constructors {[aatree.AAMap]
                   [aatree.AAMap]
                   [aatree.AAMap clojure.lang.IPersistentMap]
                   [aatree.AAMap]}
    :init init
    :state impl)
  (:require [aatree.nodes :refer :all])
  (:import (aatree AAMap AASet)
           (clojure.lang MapEntry RT IPersistentMap ISeq)
           (aatree.nodes INode)))

(set! *warn-on-reflection* true)

(defn -getState [^AASet this]
  (let [^AAMap mpl (.-impl this)]
  (.-state mpl)))

(defn -init
  ([aamap]
   [[aamap] aamap])
  ([aamap meta]
   (let [mpl (with-meta aamap meta)]
   [[mpl] mpl])))

(defn -meta [this] (get-meta this))

(defn -withMeta [^AASet this meta] (new AASet (.-impl this) meta))

(defn -disjoin [^AASet this key]
  (if (contains? this key)
    (new AASet (dissoc (.-impl this) key))
    this))

(defn -cons [^AASet this key]
  (if (contains? this key)
    this
    (new AASet (assoc (.-impl this) key key))))

(defn -empty [^AASet this]
  (new AASet (empty (.-impl this))))

(defn -rseq [^AASet this]
  (let [^AAMap mpl (.-impl this)
        ^ISeq rs (.rseq mpl)]
    (clojure.lang.APersistentMap$KeySeq/create (rs))))

(defn -comparator [^AASet this]
  (let [^AAMap mpl (.-impl this)]
    (.comparator mpl)))

(defn -entryKey [entry]
  entry)


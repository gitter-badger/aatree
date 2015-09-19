(ns aatree.lazy-nodes
  (:require [aatree.nodes :refer :all]))

(set! *warn-on-reflection* true)

(declare ->LazyNode
         ^aatree.nodes.INode get-data
         factory-for-instance
         create-lazy-empty-node)

(deftype LazyNode [data-atom sval-atom buffer-atom factory]

  aatree.nodes.INode

  (newNode [this t2 level left right cnt resources]
    (let [d (->Node t2 level left right cnt)]
      (->LazyNode (atom d) (atom nil) (atom nil) (factory-for-instance (:factory-registry resources) t2))))

  (getT2 [this resources] (.getT2 (get-data this resources) resources))

  (getLevel [this resources] (.getLevel (get-data this resources) resources))

  (getLeft [this resources] (.getLeft (get-data this resources) resources))

  (getRight [this resources] (.getRight (get-data this resources) resources))

  (getCnt [this resources] (.getCnt (get-data this resources) resources))

  (getNada [this] (create-lazy-empty-node)))

(definterface IFactory
  (qualified [t2])
  (sval [^aatree.lazy_nodes.LazyNode lazyNode resources])
  (byteLength [lazyNode resources])
  (deserialize [lazyNode resources])
  (write [lazyNode buffer resources])
  (read [lazyNode buffer resources]))

(defn- ^IFactory get-factory [^LazyNode lazy-node]
  (.-factory lazy-node))

(defn node-byte-length [lazy-node resources] (.byteLength (get-factory lazy-node) lazy-node resources))

(deftype factory-registry [by-id-atom by-type-atom])

(defn ^factory-registry create-factory-registry []
  (factory-registry. (atom {}) (atom {})))

(def default-factory-registry (create-factory-registry))

(defn- ^IFactory factory-for-id [^factory-registry fregistry id]
  (let [f (@(.-by_id_atom fregistry) id)]
  (if (nil? f)
    (factory-for-id fregistry (byte \e))
    f)))

(defn className [^Class c] (.getName c))

(defn ^IFactory factory-for-type [^factory-registry fregistry typ]
  (let [f (@(.-by_type_atom fregistry) typ)]
    (if (nil? f)
      (factory-for-id fregistry (byte \e))
      f)))

(defn- ^IFactory factory-for-instance [^factory-registry fregistry inst]
  (let [typ (type inst)
        f (factory-for-type fregistry typ)
        q (.qualified f inst)]
    (if (nil? q)
      (throw (UnsupportedOperationException. (str "Unknown qualified durable type: " (className typ))))
      q)))

(defn register-factory [^factory-registry fregistry ^IFactory factory id typ]
  (swap! (.-by-id-atom fregistry) assoc id factory)
  (if typ
    (swap! (.-by-type-atom fregistry) assoc typ factory)))

(defn- deserialize [^LazyNode this resources]
  (let [d (.deserialize (get-factory this) this resources)
        a (.-data-atom this)]
    (compare-and-set! a nil d)
    @a))

(defn- get-data [^LazyNode this resources]
  (let [d @(.-data-atom this)]
    (if (nil? d)
      (deserialize this resources)
      d)))

(register-factory
  default-factory-registry
  (reify IFactory
    (qualified [this t2] this)
    (sval [this lazyNode resources]
      (let [sval-atom (.-sval-atom lazyNode)]
        (if (nil? @sval-atom)
          (compare-and-set! sval-atom nil (pr-str (.getT2 lazyNode resources))))
        @sval-atom))
    (byteLength [this lazyNode resources]
      (+ 4 ;byte length
         (node-byte-length (left-node lazyNode resources) resources) ;left node
         4 ;sval length
         (* 2 (.sval this lazyNode resources)) ;sval
         (node-byte-length (left-node lazyNode resources) resources))) ;right node
    (deserialize [this lazyNode resources])
    (write [this lazyNode buffer resources])
    (read [this lazyNode buffer resources]))
  (byte \e)
  nil)

(def ^LazyNode lazy-node
  (->LazyNode
    (atom (create-empty-node))
    (atom "")
    (atom nil)
    (reify IFactory
      (qualified [this t2] this)
      (sval [this lazyNode resources]
        "")
      (byteLength [this lazyNode resources]
        0)
      (deserialize [this lazyNode resources])
      (write [this lazyNode buffer resources])
      (read [this lazyNode buffer resources]))))

(register-factory
  default-factory-registry
  (.factory lazy-node)
  (byte \n)
  nil)

(defn create-lazy-empty-node
  [] lazy-node)

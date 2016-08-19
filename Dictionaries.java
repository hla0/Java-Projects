
// import lists and other data structures from the Java standard library
import java.util.*;

// a type for dictionaries mapping keys of type K to values of type V
interface Dict<K,V> {
   void put(K k, V v);
   V get(K k) throws NotFoundException;
}

class NotFoundException extends Exception {
    private static final long serialVersionUID = 6729390415796943874L;
}

class DictImpl2<K,V> implements Dict<K,V> {
    protected Node<K,V> root;

    DictImpl2() { 
        root = new Empty<K,V>();
    }

    public void put(K k, V v) { 
        Node<K,V> temp = root.findNode(k);
        temp = temp.inputEntry(k,v);
    }

    public V get(K k) throws NotFoundException {
        return root.findValue(k);
    }
}

interface Node<K,V> {
    V findValue(K key) throws NotFoundException;
    public Node<K,V> findNode(K key);
    Node<K,V> inputEntry(K key, V value);
}

class Empty<K,V> implements Node<K,V> {
    Empty() {}
    public V findValue(K key) throws NotFoundException {
        throw new NotFoundException();
    }
    
    public Node<K,V> findNode(K key) {
        return this;
    }
    public Node<K,V> inputEntry(K key, V value) {
        return new Entry<K,V>(key,value,new Empty<K,V>());
    }
}

class Entry<K,V> implements Node<K,V> {
    protected K k;
    protected V v;
    protected Node<K,V> next;

    Entry(K k, V v, Node<K,V> next) {
        this.k = k;
        this.v = v;
        this.next = next;
    }
    
    public V findValue(K key) throws NotFoundException{
        if (key.equals(k)) {
            return v;
        }
        else {
            return next.findValue(key);
        }
    }
    
    public Node<K,V> findNode(K key) {
        if (key.equals(k)) {
            return this;
        }
        else {
            return next.findNode(key);
        }
    }
    
    public Node<K,V> inputEntry(K key, V value) {
        this.v = value;
        return this;
    }
}


interface DictFun<A,R> {
    R invoke(A a) throws NotFoundException;
}


class DictImpl3<K,V> implements Dict<K,V> {
    protected DictFun<K,V> dFun;

    DictImpl3() { dFun = (K key) -> {throw new NotFoundException();};  }

    public void put(K k, V v) {
        DictFun<K,V> old = dFun;
        dFun = (K key) -> {
                                    if (key.equals(k)) {
                                        return v;
                                    }
                                    else {
                                        return old.invoke(key);
                                    }
                                }; 
    }

    public V get(K k) throws NotFoundException { return dFun.invoke(k); }
}


class Pair<A,B> {
    protected A fst;
    protected B snd;

    Pair(A fst, B snd) { this.fst = fst; this.snd = snd; }

    A fst() { return fst; }
    B snd() { return snd; }
}

interface FancyDict<K,V> extends Dict<K,V> {
    void clear();
    boolean containsKey(K k);
    void putAll(List<Pair<K,V>> entries);
}

class FancyDictImpl3<K,V> implements FancyDict<K,V>{
    protected DictFun<K,V> dFun;
    FancyDictImpl3() {
        dFun = (K key) -> {throw new NotFoundException();};
    }
    
    public void clear() { 
        dFun = (K key) -> {throw new NotFoundException();};   
    }
    
    public void putAll(List<Pair<K,V>> entries) {
        for (Pair<K,V> p: entries) {
            this.put(p.fst(), p.snd());
        }
    }
    
    public void put(K k, V v) {
        DictFun<K,V> old = dFun;
        dFun = (K key) -> {
                                    if (key.equals(k)) {
                                        return v;
                                    }
                                    else {
                                        return old.invoke(key);
                                    }
                                }; 
    }
    
    public V get(K k) throws NotFoundException {
        return dFun.invoke(k);
    }
    
    public boolean containsKey(K k) {
        try {
            dFun.invoke(k);
            return true;
        }
        catch (NotFoundException e){
            return false;
        }
    }
}

class DictTest {
    public static void main(String[] args) {

    Dict<String,Integer> dict1 = new DictImpl2<String,Integer>();
    dict1.put("hello", 23);
    dict1.put("bye", 45);
    try {
        System.out.println("bye maps to " + dict1.get("bye")); // prints 45
        System.out.println("hi maps to " + dict1.get("hi"));  // throws an exception
    } catch(NotFoundException e) {
        System.out.println("not found!");  // prints "not found!"
    }

    Dict<String,Integer> dict2 = new DictImpl3<String,Integer>();
    dict2.put("hello", 23);
    dict2.put("bye", 45);
    try {
        System.out.println("bye maps to " + dict2.get("bye"));  // prints 45
        System.out.println("hi maps to " + dict2.get("hi"));   // throws an exception
    } catch(NotFoundException e) {
        System.out.println("not found!");  // prints "not found!"
    }

    FancyDict<String,Integer> dict3 = new FancyDictImpl3<String,Integer>();
    dict3.put("hello", 23);
    dict3.put("bye", 45);
    System.out.println(dict3.containsKey("bye")); // prints true
    dict3.clear();
    System.out.println(dict3.containsKey("bye")); // prints false

    }
}

package maspack.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Basic Doubly Linked-List with access to links
 * @author Antonio
 *
 */
public class LinkLinkedList<T> implements List<T>, Deque<T>, Cloneable {
   
   public static class Link<T> {
      T elem;
      Link<T> prev;
      Link<T> next;
      public Link (T elem) {
         this.prev = null;
         this.next = null;
         this.elem = elem;
      }
      
      protected void setPrev(Link<T> prev) {
         this.prev = prev;
      }
      
      protected void setNext(Link<T> next) {
         this.next = next;
      }
      
      protected Link<T> insertAfter(T elem) {
         Link<T> n = new Link<> (elem);
         insertAfter (n);
         return n;
      }
      
      protected Link<T> insertBefore(T elem) {
         Link<T> n = new Link<> (elem);
         insertBefore (n);
         return n;
      }
      
      protected void insertBefore(Link<T> e) {
         e.next = this;
         e.prev = prev;
         if (prev != null) {
            prev.next = e;
         }
         prev = e;
      }
      
      protected void insertAfter(Link<T> e) {
         e.prev = this;
         e.next = next;
         if (next != null) {
            next.prev = e;
         }
         next = e;
      }
      
      protected void remove() {
         if (next != null) {
            this.next.prev = prev;
         }
         if (prev != null) {
            this.prev.next = next;
         }
         prev = null;
         next = null;
      }
      
      public T get() {
         return elem;
      }
      
      public void set(T e) {
         elem = e;
      }
      
      /**
       * Next link in the list
       * @return the previous link
       */
      public Link<T> next() {
         return next;
      }
      
      /**
       * Previous link in the list
       * @return the previous link
       */
      public Link<T> previous() {
         return prev;
      }
   }
   
   private static class EndLink<T> extends Link<T>{
      public EndLink () {
         super (null);
      }
      
      @Override
      protected void setNext (Link<T> next) {}
      
      @Override
      public Link<T> next () {
         throw new NoSuchElementException ();
      }
      
      @Override
      protected void remove () {
         throw new IllegalArgumentException ("Cannot remove end element of LinkedList");
      }
      
   }
   
   Link<T> front;     // points to first element
   Link<T> end;    // past-the-end element
   int size;
   int modVersion;    // structural modification version 
   
   public LinkLinkedList() {
      end = new EndLink<> ();
      front = end;
      size = 0;
      modVersion = 0;
   }
   
   /**
    * Throws an exception if single-threaded concurrent modification
    * (e.g. via listIterator or subList) is detected
    */
   protected void checkConcurrentModification() {
   }
   
   /**
    * First element
    * @return first element
    */
   public Link<T> begin() {
      return front;
   }
   
   /**
    * Past-the-end element
    * @return past-the-end element
    */
   public Link<T> end() {
      return end;
   }

   @Override
   public boolean add (T e) {
      addLast(e);
      return true;
   }
   
   /**
    * Adds a link to the end of the list,
    * @param e element to add
    * @return the newly created link
    */
   public Link<T> addLink(T e) {
      return addLastLink(e);
   }
   
   /**
    * Adds a link to the end of the list
    * @param e element to add
    * @return the new link
    */
   public Link<T> addLastLink(T e) {
      Link<T> link = new Link<T>(e);
      addLastLink (link);
      return link;
   }
   
   /**
    * Adds a link to the front of the list
    * @param e element to add
    * @return the new link
    */
   public Link<T> addFirstLink(T e) {
      Link<T> link = new Link<T>(e);
      addFirstLink(link);
      return link;
   }
   
   /**
    * Adds a link to the front of the list
    * @param link link to add
    */
   public void addFirstLink(Link<T> link) {
      checkConcurrentModification ();
      insertLinkBefore (begin(), link);
   }
   
   /**
    * Adds a link to the end of the list
    * @param link link to add
    */
   public void addLastLink(Link<T> link) {
      checkConcurrentModification ();
      insertLinkBefore (end(), link);
   }
   
   /**
    * First link
    * @return the first link
    * @throws NoSuchElementException if there is no link
    */
   public Link<T> frontLink() {
      if (size == 0) {
         throw new NoSuchElementException ();
      }
      return begin ();
   }
   
   /**
    * Last link
    * @return the last link
    * @throws NoSuchElementException if there is no element in the list
    */
   public Link<T> backLink() {
      checkConcurrentModification ();
      if (size == 0) {
         throw new NoSuchElementException ();
      }
      return end().previous ();
   }
   
   /**
    * Adds an element to the END of this list
    * @param e element to add
    */
   public void addLast(T e) {
      checkConcurrentModification ();
      insertLinkBefore(end(), e);
   }
   
   /**
    * Adds an element to the BEGINNING of this list
    * @param e element to add
    */
   public void addFirst(T e) {
      checkConcurrentModification ();
      insertLinkBefore(begin(), e);
   }

   /**
    * Returns the link at the specified index
    * @param idx index
    * @return link
    */
   public Link<T> getLink(int idx) {
      checkConcurrentModification ();
      
      if (idx < 0 || idx > size) {
         throw new IndexOutOfBoundsException ();
      }
      
      Link<T> b;
      // check whether faster to go from back or front
      if (idx > size/2) {
         int bidx = size-idx;
         b = end();
         for (int i=0; i<bidx; ++i) {
            b = b.previous ();
         }
      } else {
         b = begin();
         for (int i=0; i<idx; ++i) {
            b = b.next ();
         }
      }
      return b;
   }
   
   @Override
   public void add (int idx, T e) {
      Link<T> b = getLink(idx);
      insertLinkBefore(b, e);
   }

   @Override
   public boolean addAll (Collection<? extends T> c) {
      checkConcurrentModification ();
      for (T e : c) {
         insertLinkBefore(end, e);
      }
      return true;
   }

   @Override
   public boolean addAll (int idx, Collection<? extends T> c) {
      boolean changed = false;
      Link<T> link = getLink (idx);
      for (T e : c) {
         changed = true;
         insertLinkBefore(link, e);
      }
      return changed;
   }

   private void insertLinkBefore(Link<T> a, T b) {
      Link<T> link = new Link<T>(b);
      insertLinkBefore (a, link);
   }
   
   // inserts b before a
   protected void insertLinkBefore(Link<T> a, Link<T> b) {
      if (a == front) {
         front = b;
      }
      a.insertBefore (b);
      ++size;
      ++modVersion;
   }
   
   public void removeLink(Link<T> link) {
      if (link == end) {
         throw new IllegalArgumentException ("Cannot remove end of list");
      }
      // first account for front pointer
      if (link == front) {
         front = front.next();
      }
      link.remove ();
      --size;
      ++modVersion;
   }
   
   @Override
   public void clear () {
      checkConcurrentModification ();
      
      // break all elements in the chain
      Link<T> b = front;
      for (int i=0; i<size; ++i) {
         Link<T> n = b;
         b = n.next();
         removeLink (n);
      }
      front = end;
      size = 0;
   }
   
   /**
    * Finds the first link whose get() method
    * returns the specified value (in == or .equals() sense).
    * Searches a particular range of links.
    * @param o object to check for equality
    * @param front first link to check
    * @param end last link (exclusive)
    * @return link containing value, end if not found.
    */
   public Link<T> findLink(Object o, Link<T> front, Link<T> end) {
      checkConcurrentModification ();
      Link<T> b = front;
      while (b != end) {
         T t = b.get ();
         if (t == o || 
             (t != null && t.equals (o))) {
            return b;
         }
      }
      return end;
   }
   
   /**
    * Finds the first link whose get() method
    * returns the specified value (in == or .equals() sense)
    * @param o object to check for equality
    * @return link containing value, {@link #end()} if not found.
    */
   public Link<T> findLink(Object o) {
      return findLink(o, begin(), end());
   }
   
   /**
    * Finds the last link whose get() method
    * returns the specified value (in == or .equals() sense)
    * @param o object to check for equality
    * @param front front element (last one checked)
    * @param end end of list (exclusive, not checked)
    * @return link containing value, end if not found.
    */
   public Link<T> findLastLink(Object o, Link<T> front, Link<T> end) {
      checkConcurrentModification ();
      Link<T> b = end;
      while (b != front) {
         b = b.previous ();
         
         T t = b.get ();
         if (t == o || 
             (t != null && t.equals (o))) {
            return b;
         }         
      }
      return end;
   }
   
   /**
    * Finds the last link whose get() method
    * returns the specified value (in == or .equals() sense)
    * @param o object to check for equality
    * @return link containing value, {@link #end()} if not found.
    */
   public Link<T> findLastLink(Object o) {
      return findLastLink (o, begin(), end());
   }

   @Override
   public boolean contains (Object o) {
      return (findLink(o) != end());
   }

   @Override
   public boolean containsAll (Collection<?> c) {
      for (Object e : c) {
         if (!contains(e)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public T get (int index) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException ();
      }
      return getLink (index).get ();
   }

   @Override
   public int indexOf (Object o) {
      checkConcurrentModification ();
      int idx = 0;
      Link<T> b = begin();
      while (b != end) {
         T t = b.get ();
         if (t == o || 
             (t != null && t.equals (o))) {
            return idx;
         }
         ++idx;
         b = b.next ();
      }
      return -1;
   }
   
   
   @Override
   public int lastIndexOf (Object o) {
      checkConcurrentModification ();
      int idx = size;
      Link<T> b = end;
      while (b != front) {
         --idx;
         b = b.previous ();
         
         T t = b.get ();
         if (t == o || 
             (t != null && t.equals (o))) {
            return idx;
         }         
      }
      return -1;
   }

   @Override
   public boolean isEmpty () {
      checkConcurrentModification ();
      return (size == 0);
   }
   
   private static class LinkedListIterator<T> implements ListIterator<T> {

      LinkLinkedList<T> list;
      Link<T> front;
      Link<T> end;
      Link<T> current;
      int idx;
      boolean canRemove;
      boolean lastPrev;
      boolean canSet;
      int modVersion;
      
      public LinkedListIterator (LinkLinkedList<T> list, Link<T> front, 
         Link<T> current, int currentIdx, Link<T> end) {
         this.list = list;
         modVersion = list.modVersion;
         this.front = front;
         this.end = end;
         this.current = current;
         idx = currentIdx;
         canRemove = false;
         canSet = false;
         lastPrev = false;
      }
      
      protected void checkConcurrentModification() {
         if (list.modVersion != modVersion) {
            throw new ConcurrentModificationException ();
         }
      }
      
      @Override
      public void add (T e) {
         checkConcurrentModification ();
         list.insertLinkBefore(current, e);
         modVersion = list.modVersion;
         canRemove = false;
         canSet = false;
         ++idx;
      }

      @Override
      public boolean hasNext () {
         checkConcurrentModification ();
         return (current != end);
      }

      @Override
      public boolean hasPrevious () {
         checkConcurrentModification ();
         return (current != front);
      }

      @Override
      public T next () {
         checkConcurrentModification ();
         if (current == end) {
            throw new NoSuchElementException ();
         }
         T out = current.get ();
         current = current.next();
         ++idx;
         canRemove = true;
         canSet = true;
         lastPrev = false;
         return out;
      }

      @Override
      public int nextIndex () {
         checkConcurrentModification ();
         return idx;
      }

      @Override
      public T previous () {
         checkConcurrentModification ();
         if (current == front) {
            throw new NoSuchElementException ();
         }
         current = current.previous ();
         --idx;
         canRemove = true;
         canSet = true;
         lastPrev = true;
         return current.get ();
      }

      @Override
      public int previousIndex () {
         checkConcurrentModification ();
         return idx-1;
      }

      @Override
      public void remove () {
         checkConcurrentModification ();
         if (canRemove) {
            if (lastPrev) {
               list.removeLink(current);
            } else {
               list.removeLink (current.previous ());
            }
            modVersion = list.modVersion;
            canRemove = false;
            canSet = false;
         } else {
            throw new IllegalStateException ();
         }
         --idx;
      }

      @Override
      public void set (T e) {
         checkConcurrentModification ();
         if (canSet) {
            if (lastPrev) {
               current.set (e);
            } else {
               current.previous ().set (e);
            }
         } else {
            throw new IllegalStateException ();
         }
      }
   }
   
   private static class DescendingLinkLinkedListIterator<T> extends LinkedListIterator<T> {

      public DescendingLinkLinkedListIterator (LinkLinkedList<T> list, Link<T> front,
      Link<T> current, int currentIdx, Link<T> end) {
         super (list, front, current, currentIdx, end);
      }

      @Override
      public boolean hasPrevious () {
         return super.hasNext ();
      }
      
      @Override
      public T previous () {
         return super.next ();
      }
      
      @Override
      public int previousIndex () {
         return super.nextIndex ();
      }
      
      @Override
      public void add (T e) {
         super.add (e);
         super.previous (); // go back so that newly added will be next "previous"
      }
      
      @Override
      public boolean hasNext () {
         return super.hasPrevious ();
      }
      
      @Override
      public T next () {
         return super.previous ();
      }
      
      @Override
      public int nextIndex () {
         return super.previousIndex ();
      }
     
      
   }

   @Override
   public Iterator<T> iterator () {
      return listIterator ();
   }

   @Override
   public ListIterator<T> listIterator () {
      checkConcurrentModification ();
      return new LinkedListIterator<T>(this, begin(), begin(), 0, end());
   }

   @Override
   public ListIterator<T> listIterator (int idx) {
      checkConcurrentModification ();
      Link<T> p = getLink (idx);
      return new LinkedListIterator<T>(this, begin(), p, idx, end());
   }

   @Override
   public boolean remove (Object o) {
      Link<T> link = findLink(o);
      if (link != end()) {
         removeLink(link);
         return true;
      }
      return false;
   }

   @Override
   public T remove (int idx) {
      Link<T> link = getLink (idx);
      removeLink(link);
      return link.get ();
   }

   @Override
   public boolean removeAll (Collection<?> c) {
      boolean modified = false;
      for (Object e : c) {
         modified |= remove(e);
      }
      return modified;
   }

   @Override
   public boolean retainAll (Collection<?> c) {
      checkConcurrentModification ();
      boolean modified = false;
      
      Link<T> link = begin();
      while (link != end) {
         if (!c.contains (link.get ())) {
            Link<T> next = link.next ();
            removeLink(link);
            link = next;
            modified = true;
         } else {
            link = link.next();
         }
      }
      return modified;
   }

   @Override
   public T set (int idx, T element) {
      Link<T> link = getLink (idx);
      T old = link.get ();
      link.set (element);
      return old;
   }

   @Override
   public int size () {
      checkConcurrentModification ();
      return size;
   }
   
   private static class LinkedSubList<T> extends LinkLinkedList<T> {
      LinkLinkedList<T> backing;
      
      public LinkedSubList(LinkLinkedList<T> backing, Link<T> front, Link<T> end, int size) {
         super();
         this.modVersion = backing.modVersion;
         this.backing = backing;
         this.front = front;
         this.end = end;
         this.size = size;
      }
      
      protected void checkConcurrentModification() {
         if (this.modVersion != super.modVersion) {
            throw new ConcurrentModificationException ();
         }
      }
     
      // inserts b before a
      protected void insertLinkBefore(Link<T> a, Link<T> b) {
         backing.insertLinkBefore (a, b);
         ++size;
         this.modVersion = backing.modVersion;
      }
      
      public void removeLink(Link<T> link) {
         backing.removeLink (link);
         --size;
         this.modVersion = backing.modVersion;
      }
      
      /**
       * Another sub-list, with the same backing list
       */
      @Override
      public LinkedSubList<T> clone () {
         return new LinkedSubList<> (backing, front, end, size);
      }
   }

   @Override
   public LinkLinkedList<T> subList (int fromIndex, int toIndex) {
      Link<T> first = getLink(fromIndex);
      Link<T> end = getLink(toIndex);
      return new LinkedSubList<T>(this, first, end, toIndex-fromIndex);
   }

   @Override
   public Object[] toArray () {
      Object[] array = new Object[size()];
      
      int idx = 0;
      Link<T> link = begin();
      while (link != end) {
         array[idx] = link.get ();
         ++idx;
      }
      return array;
   }

   @Override
   public <E> E[] toArray (E[] a) {
      int size = size();
      @SuppressWarnings("unchecked")
      E[] array = a.length >= size ? a :
            (E[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
      
      int idx = 0;
      Link<T> link = begin();
      while (link != end) {
         @SuppressWarnings("unchecked")
         E e = (E)(link.get ());
         array[idx] = e;
         ++idx;
      }
      return array;
      
   }

   @Override
   public Iterator<T> descendingIterator () {
      checkConcurrentModification ();
      return new DescendingLinkLinkedListIterator<T>(this, begin(), end(), size, end());
   }

   @Override
   public T element () {
      return getFirst();
   }

   @Override
   public T getFirst () {
      checkConcurrentModification ();
      if (size == 0) {
         throw new NoSuchElementException ();
      }
      return front.get ();
   }

   @Override
   public T getLast () {
      checkConcurrentModification ();
      if (size == 0) {
         throw new NoSuchElementException ();
      }
      return end.previous().get ();
   }

   @Override
   public boolean offer (T e) {
      addLast(e);
      return true;
   }

   @Override
   public boolean offerFirst (T e) {
      addLast(e);
      return true;
   }

   @Override
   public boolean offerLast (T e) {
      return offer(e);
   }

   @Override
   public T peek () {
      return peekFirst();
   }

   @Override
   public T peekFirst () {
      checkConcurrentModification ();
      if (size == 0) {
         return null;
      }
      return front.get ();
   }

   @Override
   public T peekLast () {
      checkConcurrentModification ();
      if (size == 0) {
         return null;
      }
      return end.previous().get ();
   }

   @Override
   public T poll () {
      return pollFirst();
   }

   @Override
   public T pollFirst () {
      T first = peekFirst ();
      if (front != end) {
         removeLink (front);
      }
      return first;
   }

   @Override
   public T pollLast () {
      T first = peekLast ();
      if (size != 0) {
         removeLink (end.previous ());
      }
      return first;
   }

   @Override
   public T pop () {
      return removeFirst();
   }

   @Override
   public void push (T e) {
      addFirst (e);
   }

   @Override
   public T remove () {
      return removeFirst();
   }

   @Override
   public T removeFirst () {
      T first = getFirst ();
      if (front != end) {
         removeLink (front);
      }
      return first;
   }

   @Override
   public boolean removeFirstOccurrence (Object o) {
      Link<T> first = findLink (o);
      if (first != end) {
         removeLink (first);
         return true;
      }
      return false;
   }

   @Override
   public T removeLast () {
      T last = getLast ();
      if (front != end) {
         removeLink (front);
      }
      return last;
   }

   @Override
   public boolean removeLastOccurrence (Object o) {
      Link<T> last = findLastLink (o);
      if (last != end) {
         removeLink (last);
         return true;
      }
      return false;
   }
   
   /**
    * Clones the list (shallow copy)
    */
   @Override
   public LinkLinkedList<T> clone () {
      LinkLinkedList<T> out = new LinkLinkedList<> ();
      Link<T> b = begin();
      while (b != end) {
         out.add (b.get());
         b = b.next();
      }
      return out;
   }
   

}

package cs2030.lambda;

import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A InfiniteList is a list that supports functional operations
 * generate, iterate, map, filter, reduce, findFirst, limit, count,
 * zipWith, unzipTo, takeWhile, and forEach.   A InfiniteLIst is
 * immutable.  It is _lazily_ evaluated.
 */
public class InfiniteList<T> {
  /** The supplier of the head. */
  public Supplier<T> headSupplier;

  /** The supplier of the tail (rest of the list). */
  public Supplier<InfiniteList<T>> tailSupplier;

  /** A cached value of the head. */
  public Optional<T> headValue;

  /** A cached value of the tail. */
  public Optional<InfiniteList<T>> tailValue;

  /** 
   * Indicate if the head value is filtered. 
   * It could be Optional.of(true)/Optional.of(false), in which case
   * the predicate has been evaluated and we are caching the result
   * of the predicate.  It could also be Optional.empty(), which 
   * corresponds on the case where there is no filter, or there is a
   * filter but it has not been evaluated.
   */
  public Optional<Boolean> headIsFiltered;

  /** Indicate if the head value is filtered. */
  public Optional<Predicate<T>> headFilter;

  /**
   * InfiniteList has a private constructor to prevent the list
   * from created directly.
   */
  private InfiniteList() { }

  /**
   * Empty is a special private subclass of InfiniteList that
   * corresponds to an empty InfiniteList.  We intentionally
   * violate LSP here, so that it throws an error if we try
   * to use an empty list like a normal list.
   */
  private static class Empty<T> extends InfiniteList<T> {
    @Override
    public T head() {
      throw new IllegalStateException("calling head() on empty list");
    }

    @Override
    public InfiniteList<T> tail() {
      throw new IllegalStateException("calling tail() on empty list");
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public <R> InfiniteList<R> map(Function<? super T, ? extends R> mapper) {
      return InfiniteList.empty();
    }

    @Override
    public InfiniteList<T> limit(int n) {
      return this;
    }

    @Override
    public InfiniteList<T> takeWhile(Predicate<T> predicate) {
      return this;
    }

    @Override
    public InfiniteList<T> filter(Predicate<T> predicate) {
      return this;
    }

    @Override
    public String toString() {
      return "-";
    }
  }

  /**
   * A private constructor that takes in two suppliers, and a boolean
   * to indicate if the head exists or not.
   */
  InfiniteList(Supplier<T> head, Supplier<InfiniteList<T>> tail, 
      Optional<Boolean> headIsFiltered) {
    this.headSupplier = head;
    this.headValue = Optional.empty();
    this.headFilter = Optional.empty();
    this.headIsFiltered = headIsFiltered;
    this.tailSupplier = tail;
    this.tailValue = Optional.empty();
  }

  InfiniteList(T head, Supplier<InfiniteList<T>> tail,
      Optional<Boolean> headIsFiltered) {
    this.headSupplier = () -> head;
    this.headValue = Optional.of(head);
    this.headFilter = Optional.empty();
    this.headIsFiltered = headIsFiltered;
    this.tailSupplier = tail;
    this.tailValue = Optional.empty();
  }

  InfiniteList(InfiniteList<T> list, Supplier<InfiniteList<T>> tail, 
      Predicate<T> filter, Optional<Boolean> filtered) {
    this.headSupplier = list.headSupplier;
    this.headValue = list.headValue;
    this.headFilter = Optional.of(filter);
    this.headIsFiltered = filtered;
    this.tailSupplier = tail;
    this.tailValue = Optional.empty();
  }

  /**
   * A private constructor that takes in two suppliers.  The head
   * is not filtered.
   */
  InfiniteList(Supplier<T> head, Supplier<InfiniteList<T>> tail) {
    this.headSupplier = head;
    this.headValue = Optional.empty();
    this.headIsFiltered = Optional.empty();
    this.headFilter = Optional.empty();
    this.tailSupplier = tail;
    this.tailValue = Optional.empty();
  }

  /**
   * A private constructor that takes in two suppliers.  The head
   * is not filtered.
   */
  InfiniteList(T head, Supplier<InfiniteList<T>> tail) {
    this.headSupplier = () -> head;
    this.headValue = Optional.ofNullable(head);
    this.headIsFiltered = Optional.empty();
    this.headFilter = Optional.empty();
    this.tailSupplier = tail;
    this.tailValue = Optional.empty();
  }

  /**
   * Return the head of the list.  If the head is not evaluated yet,
   * the supplier is called.  Internally, the list increases the
   * number of evaluations by one.  The value is cached.
   * @return The head of the list.
   */
  public T head() {
    return this.headValue.orElseGet(() -> {
      T head = this.headSupplier.get();
      this.headValue = Optional.of(head);
      return head;
    });
  }

  /**
   * Return the tail of the list, which is another InfiniteList.
   * If the tail is not evaluated yet, the supplier is called and
   * the value is cached.
   * @return The tail of the list.
   */
  public InfiniteList<T> tail() {
    InfiniteList<T> list = this.tailValue.orElseGet(this.tailSupplier);
    this.tailValue = Optional.of(list);
    return list;
  }

  /**
   * Return if the head is filtered. 
   * If the filter predicate for this head is not evaluated yet, the predicate
   * is called and the value is cached.
   * @return true if this head is filtered.
   */
  public boolean headIsFiltered() {
    boolean filtered = this.headIsFiltered.orElseGet(() -> {
      if (!headFilter.isPresent()) {
        return false;
      } else {
        return !this.headFilter.get().test(this.head());
      }
    });
    return filtered;
  }

  /**
   * Create an empty InfiniteList.
   * @return An empty InfiniteList.
   */
  public static <T> InfiniteList<T> empty() {
    return new Empty<T>();
  }

  /**
   * Checks if the list is empty.
   * @return true if the list is empty; false otherwise.
   */
  public boolean isEmpty() {
    // An InfiniteList not empty unless it is an instance of Empty.
    if (this.headIsFiltered()) {
      return this.tail().isEmpty();
    }
    return false;
  }

  /**
   * Generate an infinite list elements, each is generated with
   * the supplier s.
   * @param <T> The type of elements to generate.
   * @param supply A supplier function to generate the elements.
   * @return A new list generated.
   */
  public static <T> InfiniteList<T> generate(Supplier<T> supply) {
    return new InfiniteList<T>(supply, () -> InfiniteList.generate(supply));
  }

  /**
   * Generate an infinite list elements, starting with init
   * and next element computed with the {@code next} function.
   * @param <T> The type of elements to generate.
   * @param next A function to generate the next element.
   * @return A new list generated.
   */
  public static <T> InfiniteList<T> iterate(T init, Function<T, T> next) {
    return new InfiniteList<T>(init, () -> InfiniteList.iterate(next.apply(init), next));
  }

  /**
   * Return the first element that matches the given predicate, or
   * Optional.empty() if none of the elements matches.
   * @param  predicate A predicate to apply to each element to determine
   *     if it should be returned.
   * @return An Optional object containing either the first element
   *     that matches, or is empty if none of the element matches.
   */
  public Optional<T> findFirst(Predicate<T> predicate) {
    InfiniteList<T> list = this;
    while (!list.isEmpty()) {
      if (!list.headIsFiltered()) {
        T h = list.head();
        if (predicate.test(h)) {
          return Optional.of(h);
        }
      }
      list = list.tail();
    }
    return Optional.empty();
  }

  /**
   * Returns a list consisting of the results of applying the given function
   * to the elements of this list.
   * @param <R> The type of elements returned.
   * @param mapper The function to apply to each element.
   * @return The new list.
   */
  public <R> InfiniteList<R> map(Function<? super T, ? extends R> mapper) {
    return new InfiniteList<R>(
        () -> mapper.apply(this.head()),
        () -> this.tail().map(mapper),
        this.headIsFiltered);
  }

  /**
   * Reduce the elements of this stream to a single output, by successively
   * "accumuating" the elements using the given accumulation function.
   *
   * @param <U> The type of the value the list is reduced into.
   * @param identity The identity (initialized the accumulated values)
   * @param accumulator A function that accumulate elements in the stream.
   * @return The accumulated value.
   */
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator) {
    U results = identity;
    InfiniteList<T> list = this;
    while (!list.isEmpty()) {
      if (!list.headIsFiltered()) {
        T h = list.head();
        results = accumulator.apply(results, h);
      }
      list = list.tail();
    }
    return results;
  }

  /**
   * Truncate the list to up to n elements.  If the list has less than n
   * elements, then the original list is returned.
   * @return The truncated list.
   */
  public InfiniteList<T> limit(int n) {
    Supplier<InfiniteList<T>> tailSupplier;
    if (n == 0) {
      return InfiniteList.empty();
    }
    if (this.headIsFiltered()) {
      return new InfiniteList<T>(() -> this.head(), () -> this.tail().limit(n), Optional.of(true));
    }
    if (n == 1) {
      tailSupplier = () -> InfiniteList.empty();
    } else {
      tailSupplier = () -> this.tail().limit(n - 1);
    }
    if (this.headValue.isPresent()) {
      return new InfiniteList<T>(this.headValue.get(), tailSupplier);
    } else {
      return new InfiniteList<T>(() -> this.head(), tailSupplier);
    }
  }

  /**
   * Return a new list consisting of elements from this list
   * by successively copying the elements, until the predicate
   * becomes false.  All elements in the returned list passed
   * the predicate.
   * @return The new list.
   */
  public InfiniteList<T> takeWhile(Predicate<T> predicate) {
    boolean keepHead = !this.headIsFiltered();
    if (keepHead) {
      T head = this.head();
      if (!predicate.test(head)) {
        return InfiniteList.empty();
      }
      return new InfiniteList<T>(head, () -> this.tail().takeWhile(predicate));
    } 
    return new InfiniteList<T>(() -> this.head(), 
        () -> this.tail().takeWhile(predicate), Optional.of(true));
  }

  /**
   * Returns a list consisting of the elements of this list that
   * match the given predicate.
   * @param  predicate A predicate to apply to each element to
   *     determine if it should be included
   * @return The new list.
   */
  public InfiniteList<T> filter(Predicate<T> predicate) {
    if (this.headIsFiltered()) {
      return new InfiniteList<T>(this, () -> this.tail().filter(predicate), 
          predicate, Optional.of(true));
    } else {
      return new InfiniteList<T>(this, () -> this.tail().filter(predicate), 
          predicate, Optional.empty());
    }

    // if this.headIsFiltered() is true, then it will return 
  }

  /**
   * Return the number of elements in this list.
   * @return The number of elements in the list.
   */
  public int count() {
    int i = 0;
    InfiniteList<T> list = this;
    while (!list.isEmpty()) {
      if (!list.headIsFiltered()) {
        i++;
      }
      list = list.tail();
    }
    return i;
  }

  /**
   * Return an array containing the elements in the list.
   * @return The array containing the elements in the list.
   */
  public Object[] toArray() {
    ArrayList<Object> a = new ArrayList<>();
    InfiniteList<T> list = this;
    while (!list.isEmpty()) {
      if (!list.headIsFiltered()) {
        a.add(list.head());
      }
      list = list.tail();
    }
    return a.toArray();
  }

  /**
   * Return this infinite list in string format.
   */
  public String toString() {
    // TODO: You may need to modifiy this
    String tail = this.tailValue
        .map(x -> x.toString())
        .orElse("?");
    String head = this.headValue
        .map(x -> x.toString())
        .orElse("?");

    return head + "," + tail;
  }
}

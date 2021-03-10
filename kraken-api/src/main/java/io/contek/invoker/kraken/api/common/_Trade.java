package io.contek.invoker.kraken.api.common;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class _Trade {
  public double price;
  public double volume;
  public double time;
  public String side;
  public String orderType;
  public String misc;
}

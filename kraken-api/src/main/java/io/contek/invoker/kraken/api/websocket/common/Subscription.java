package io.contek.invoker.kraken.api.websocket.common;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class Subscription {

  public Integer depth;
  public Integer interval;
  public String name;
  public Boolean ratecounter;
  public Boolean snapshot;
  public String token;
}

package io.contek.invoker.coinbasepro.api.websocket;

import com.google.common.collect.ImmutableList;
import io.contek.invoker.coinbasepro.api.websocket.common.WebSocketChannelInfo;
import io.contek.invoker.coinbasepro.api.websocket.common.WebSocketMessage;
import io.contek.invoker.coinbasepro.api.websocket.common.WebSocketSubscriptionMessage;
import io.contek.invoker.commons.websocket.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicReference;

import static io.contek.invoker.coinbasepro.api.websocket.common.constants.WebSocketMessageKeys.*;
import static io.contek.invoker.commons.websocket.SubscriptionState.*;

@ThreadSafe
public abstract class WebSocketChannel<Message extends WebSocketMessage>
    extends BaseWebSocketChannel<Message> {

  private final String name;
  private final String productId;

  private final AtomicReference<WebSocketSubscriptionMessage> pendingRequestHolder =
      new AtomicReference<>();

  protected WebSocketChannel(String name, String productId) {
    super(id);
    this.name = name;
    this.productId = productId;
  }

  @Override
  protected final BaseWebSocketChannelId getId() {
    return name + ':' + productId;
  }

  @Override
  protected final SubscriptionState subscribe(WebSocketSession session) {
    synchronized (pendingRequestHolder) {
      WebSocketSubscriptionMessage request = new WebSocketSubscriptionMessage();
      request.type = _subscribe;
      request.channels = ImmutableList.of(getChannelInfo());
      session.send(request);
      pendingRequestHolder.set(request);
      return SUBSCRIBING;
    }
  }

  @Override
  protected final SubscriptionState unsubscribe(WebSocketSession session) {
    synchronized (pendingRequestHolder) {
      WebSocketSubscriptionMessage request = new WebSocketSubscriptionMessage();
      request.type = _unsubscribe;
      request.channels = ImmutableList.of(getChannelInfo());
      session.send(request);
      pendingRequestHolder.set(request);
      return UNSUBSCRIBING;
    }
  }

  @Nullable
  @Override
  protected final SubscriptionState getState(AnyWebSocketMessage message) {
    synchronized (pendingRequestHolder) {
      WebSocketSubscriptionMessage pendingRequest = pendingRequestHolder.get();
      if (pendingRequest == null) {
        return null;
      }
      if (!(message instanceof WebSocketSubscriptionMessage)) {
        return null;
      }
      WebSocketSubscriptionMessage casted = (WebSocketSubscriptionMessage) message;
      if (!_subscriptions.equals(casted.type)) {
        return null;
      }

      if (_subscribe.equals(pendingRequest.type)) {
        if (casted.channels.stream()
            .anyMatch(
                channel -> channel.name.equals(name) && channel.product_ids.contains(productId))) {
          pendingRequestHolder.set(null);
          return SUBSCRIBED;
        }
        return null;
      }
      if (_unsubscribe.equals(pendingRequest.type)) {
        if (casted.channels.stream()
            .anyMatch(
                channel -> channel.name.equals(name) && channel.product_ids.contains(productId))) {
          return null;
        }
        pendingRequestHolder.set(null);
        return UNSUBSCRIBED;
      }
      throw new IllegalStateException(pendingRequest.type);
    }
  }

  @Override
  protected final void reset() {
    synchronized (pendingRequestHolder) {
      pendingRequestHolder.set(null);
    }
  }

  private WebSocketChannelInfo getChannelInfo() {
    WebSocketChannelInfo channel = new WebSocketChannelInfo();
    channel.name = name;
    channel.product_ids = ImmutableList.of(productId);
    return channel;
  }
}

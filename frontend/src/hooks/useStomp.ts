import { useCallback, useEffect, useRef, useState } from "react";
import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

interface UseStompOptions {
  token?: string | null;
  guestToken?: string | null;
  enabled?: boolean;
}

interface SubscribeParams<T> {
  destination: string;
  onMessage: (payload: T, rawMessage: IMessage) => void;
}

interface UseStompResult {
  isConnected: boolean;
  subscribe: <T>(params: SubscribeParams<T>) => () => void;
}

const useStomp = ({
  token,
  guestToken,
  enabled = true,
}: UseStompOptions): UseStompResult => {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const headers: Record<string, string> = {};

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    if (guestToken) {
      headers["Guest-Token"] = guestToken;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: headers,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setIsConnected(true);
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: () => {
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      subscriptionsRef.current.forEach((subscription) => subscription.unsubscribe());
      subscriptionsRef.current = [];
      setIsConnected(false);
      client.deactivate();
      clientRef.current = null;
    };
  }, [enabled, guestToken, token]);

  const subscribe = useCallback(
    <T,>({ destination, onMessage }: SubscribeParams<T>) => {
      const client = clientRef.current;

      if (!client || !client.connected) {
        return () => undefined;
      }

      const subscription = client.subscribe(destination, (message) => {
        try {
          const parsed = JSON.parse(message.body) as T;
          onMessage(parsed, message);
        } catch {
          // ignore malformed payload
        }
      });

      subscriptionsRef.current.push(subscription);

      return () => {
        subscription.unsubscribe();
        subscriptionsRef.current = subscriptionsRef.current.filter(
          (item) => item !== subscription
        );
      };
    },
    []
  );

  return {
    isConnected,
    subscribe,
  };
};

export default useStomp;
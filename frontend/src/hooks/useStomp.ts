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

interface SubscriptionEntry {
  id: string;
  destination: string;
  handler: (message: IMessage) => void;
  subscription: StompSubscription | null;
}

const buildWsUrl = () => {
  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim();

  if (!apiBaseUrl) {
    return "/ws";
  }

  if (apiBaseUrl.startsWith("http://") || apiBaseUrl.startsWith("https://")) {
    const normalizedApiBaseUrl = apiBaseUrl.replace(/\/+$/, "");

    if (normalizedApiBaseUrl.endsWith("/api/v1")) {
      return `${normalizedApiBaseUrl.slice(0, -"/api/v1".length)}/ws`;
    }

    return `${normalizedApiBaseUrl}/ws`;
  }

  const normalizedApiBaseUrl = apiBaseUrl.replace(/\/+$/, "");

  if (normalizedApiBaseUrl.endsWith("/api/v1")) {
    return `${normalizedApiBaseUrl.slice(0, -"/api/v1".length)}/ws`;
  }

  return `${normalizedApiBaseUrl}/ws`;
};

const useStomp = ({
  token,
  guestToken,
  enabled = true,
}: UseStompOptions): UseStompResult => {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, SubscriptionEntry>>(new Map());
  const [isConnected, setIsConnected] = useState(false);

  const resubscribeAll = useCallback(() => {
    const client = clientRef.current;

    if (!client || !client.connected) {
      return;
    }

    subscriptionsRef.current.forEach((entry) => {
      if (entry.subscription) {
        try {
          entry.subscription.unsubscribe();
        } catch {
          // ignore unsubscribe errors during reconnect
        }
      }

      entry.subscription = client.subscribe(entry.destination, entry.handler);
    });
  }, []);

  useEffect(() => {
    if (!enabled) {
      subscriptionsRef.current.forEach((entry) => {
        if (entry.subscription) {
          try {
            entry.subscription.unsubscribe();
          } catch {
            // ignore cleanup errors
          }
        }
      });

      subscriptionsRef.current.clear();
      setIsConnected(false);
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
      webSocketFactory: () => new SockJS(buildWsUrl()),
      connectHeaders: headers,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setIsConnected(true);
        resubscribeAll();
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

    clientRef.current = client;
    client.activate();

    return () => {
      setIsConnected(false);

      subscriptionsRef.current.forEach((entry) => {
        if (entry.subscription) {
          try {
            entry.subscription.unsubscribe();
          } catch {
            // ignore cleanup errors
          }
        }
      });

      subscriptionsRef.current.clear();

      void client.deactivate();
      clientRef.current = null;
    };
  }, [enabled, guestToken, resubscribeAll, token]);

  const subscribe = useCallback(
    <T,>({ destination, onMessage }: SubscribeParams<T>) => {
      const id = `${destination}::${Math.random().toString(36).slice(2)}`;

      const handler = (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body) as T;
          onMessage(parsed, message);
        } catch {
          // ignore malformed payload
        }
      };

      const entry: SubscriptionEntry = {
        id,
        destination,
        handler,
        subscription: null,
      };

      subscriptionsRef.current.set(id, entry);

      const client = clientRef.current;
      if (client && client.connected) {
        entry.subscription = client.subscribe(destination, handler);
      }

      return () => {
        const currentEntry = subscriptionsRef.current.get(id);

        if (!currentEntry) {
          return;
        }

        if (currentEntry.subscription) {
          try {
            currentEntry.subscription.unsubscribe();
          } catch {
            // ignore unsubscribe errors
          }
        }

        subscriptionsRef.current.delete(id);
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
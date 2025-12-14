import {useEffect, useRef} from 'react';
import type {FunctionStatus} from '../types';

export interface FunctionStatusEvent {
    functionId: string;
    status: FunctionStatus;
    errorMessage?: string | null;
}

interface UseFunctionStatusEventsOptions {
    projectId: string;
    enabled?: boolean;
    onStatusUpdate: (event: FunctionStatusEvent) => void;
}

/**
 * Custom hook to subscribe to Server-Sent Events for function status updates
 */
export function useFunctionStatusEvents({
                                            projectId,
                                            enabled = true,
                                            onStatusUpdate,
                                        }: UseFunctionStatusEventsOptions) {
    const eventSourceRef = useRef<EventSource | null>(null);
    const onStatusUpdateRef = useRef(onStatusUpdate);

    useEffect(() => {
        onStatusUpdateRef.current = onStatusUpdate;
    }, [onStatusUpdate]);

    useEffect(() => {
        if (!enabled || !projectId) {
            return;
        }

        const eventSource = new EventSource(
            `http://localhost:8080/functions/events`,
            {withCredentials: false}
        );

        eventSourceRef.current = eventSource;

        eventSource.addEventListener('connected', (event) => {
            console.log('[SSE] Connected to function status updates', event.data);
        });

        eventSource.addEventListener('status', (event) => {
            try {
                const data: FunctionStatusEvent = JSON.parse(event.data);
                console.log('[SSE] Status update received:', data);
                onStatusUpdateRef.current(data);
            } catch (error) {
                console.error('[SSE] Failed to parse status event:', error);
            }
        });

        eventSource.onerror = (error) => {
            console.error('[SSE] Connection error:', error);
        };

        return () => {
            console.log('[SSE] Disconnecting from function status updates');
            eventSource.close();
            eventSourceRef.current = null;
        };
    }, [projectId, enabled]);

    return {
        isConnected: eventSourceRef.current?.readyState === EventSource.OPEN,
    };
}

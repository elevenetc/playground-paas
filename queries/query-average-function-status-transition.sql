SELECT from_status,
       to_status,
       AVG(duration_ms) / 1000.0 AS avg_duration_seconds,
       COUNT(*)                  AS transition_count
FROM function_status_history
WHERE duration_ms IS NOT NULL
GROUP BY from_status, to_status
ORDER BY avg_duration_seconds DESC;
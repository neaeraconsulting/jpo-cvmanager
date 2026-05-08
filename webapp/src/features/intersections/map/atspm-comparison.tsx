import { useEffect, useMemo, useState, type MouseEvent as ReactMouseEvent } from 'react'
import { Box, CircularProgress, Typography } from '@mui/material'

import AtspmApi from '../../../apis/intersections/atspm-api'
import EnvironmentVars from '../../../EnvironmentVars'

type AtspmComparisonProps = {
  token?: string
  intersectionId?: number
  startTime?: Date
  endTime?: Date
  selectedFeature?: {
    feature?: {
      layer?: { id?: string }
      properties?: { signalGroupId?: number }
    }
  }
}

type TimelinePoint = {
  ts: number
  indication: AtspmSignalIndication
}

type TimelineSegment = {
  start: number
  end: number
  indication: AtspmSignalIndication
}

const SIGNAL_COLORS: Record<string, string> = {
  GREEN: '#2e7d32',
  YELLOW: '#fbc02d',
  RED: '#d32f2f',
  UNKNOWN: '#9e9e9e',
}

const parseMillis = (value?: unknown): number | undefined => {
  if (value === undefined || value === null) return undefined

  if (value instanceof Date) {
    return value.getTime()
  }

  if (typeof value === 'object') {
    const dateValue = (value as { $date?: unknown }).$date
    if (dateValue !== undefined) return parseMillis(dateValue)

    const epochSecond = (value as { epochSecond?: unknown }).epochSecond
    const nano = (value as { nano?: unknown }).nano
    if (epochSecond !== undefined) {
      const seconds = Number(epochSecond)
      const nanos = Number(nano ?? 0)
      if (!Number.isNaN(seconds) && !Number.isNaN(nanos)) {
        return seconds * 1000 + Math.floor(nanos / 1_000_000)
      }
    }

    const seconds = (value as { seconds?: unknown }).seconds
    const nanos = (value as { nanos?: unknown }).nanos
    if (seconds !== undefined) {
      const secNum = Number(seconds)
      const nanoNum = Number(nanos ?? 0)
      if (!Number.isNaN(secNum) && !Number.isNaN(nanoNum)) {
        return secNum * 1000 + Math.floor(nanoNum / 1_000_000)
      }
    }
  }

  const asNumber = Number(value)
  if (!Number.isNaN(asNumber)) {
    return asNumber > 1000000000000 ? asNumber : asNumber * 1000
  }

  const parsed = Date.parse(String(value))
  return Number.isNaN(parsed) ? undefined : parsed
}

const normalizeIndication = (value?: unknown): AtspmSignalIndication => {
  if (value === undefined || value === null) return 'UNKNOWN'

  if (typeof value === 'number') {
    // Common ATSPM phase event codes used in many deployments
    if (value === 1 || value === 10) return 'GREEN'
    if (value === 8) return 'YELLOW'
    if (value === 11) return 'RED'
  }

  if (typeof value === 'object') {
    const name = (value as { name?: unknown }).name
    const eventState = (value as { eventState?: unknown }).eventState
    const code = (value as { code?: unknown }).code
    const valueField = (value as { value?: unknown }).value
    const candidates = [name, eventState, code, valueField]
    for (const candidate of candidates) {
      const normalized = normalizeIndication(candidate)
      if (normalized !== 'UNKNOWN') return normalized
    }
    return 'UNKNOWN'
  }

  const upper = String(value).toUpperCase()
  if (
    upper.includes('GREEN') ||
    upper.includes('PROTECTED_MOVEMENT_ALLOWED') ||
    upper.includes('PERMISSIVE_MOVEMENT_ALLOWED')
  )
    return 'GREEN'
  if (upper.includes('YELLOW') || upper.includes('CLEARANCE')) return 'YELLOW'
  if (upper.includes('RED') || upper.includes('STOP_AND_REMAIN') || upper.includes('STOP')) return 'RED'
  return 'UNKNOWN'
}

const sameSignalGroup = (a?: unknown, b?: unknown): boolean => {
  const aa = Number(a)
  const bb = Number(b)
  return !Number.isNaN(aa) && !Number.isNaN(bb) && aa === bb
}

const buildSegments = (points: TimelinePoint[], windowStart: number, windowEnd: number): TimelineSegment[] => {
  if (windowEnd <= windowStart) return []
  const sorted = [...points].sort((a, b) => a.ts - b.ts)
  if (sorted.length === 0) {
    return [{ start: windowStart, end: windowEnd, indication: 'UNKNOWN' }]
  }

  const inside = sorted.filter((p) => p.ts >= windowStart && p.ts <= windowEnd)
  const prior = [...sorted].reverse().find((p) => p.ts < windowStart)

  const effective = [...(prior ? [{ ts: windowStart, indication: prior.indication }] : []), ...inside].sort(
    (a, b) => a.ts - b.ts
  )

  if (effective.length === 0) {
    return [{ start: windowStart, end: windowEnd, indication: 'UNKNOWN' }]
  }

  const segments: TimelineSegment[] = []
  for (let i = 0; i < effective.length; i++) {
    const current = effective[i]
    const next = effective[i + 1]
    const start = Math.max(current.ts, windowStart)
    const end = Math.min(next?.ts ?? windowEnd, windowEnd)
    if (end > start) {
      segments.push({ start, end, indication: current.indication })
    }
  }

  if (segments.length === 0) {
    return [{ start: windowStart, end: windowEnd, indication: 'UNKNOWN' }]
  }
  return segments
}

const TimelineBar = ({
  label,
  segments,
  windowStart,
  windowEnd,
}: {
  label: string
  segments: TimelineSegment[]
  windowStart: number
  windowEnd: number
}) => {
  const total = Math.max(windowEnd - windowStart, 1)
  const tickCount = 6
  const ticks = Array.from({ length: tickCount + 1 }, (_, i) => i / tickCount)
  const [hoveredTs, setHoveredTs] = useState<number | null>(null)
  const [hoverPercent, setHoverPercent] = useState(0)

  const onBarMouseMove = (event: ReactMouseEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect()
    const x = event.clientX - rect.left
    const clampedX = Math.min(Math.max(x, 0), rect.width)
    const percent = rect.width > 0 ? clampedX / rect.width : 0
    setHoverPercent(percent)
    setHoveredTs(Math.round(windowStart + percent * total))
  }

  const onBarMouseLeave = () => {
    setHoveredTs(null)
  }

  return (
    <Box sx={{ mb: 1.5, pr: 1 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
        }}
      >
        <Typography fontSize="13px" sx={{ width: 50, textAlign: 'right', flexShrink: 0 }}>
          {label}
        </Typography>

        <Box sx={{ position: 'relative', flex: 1 }}>
          {hoveredTs !== null ? (
            <Box
              sx={{
                position: 'absolute',
                left: `${hoverPercent * 100}%`,
                top: -24,
                transform: 'translateX(-50%)',
                px: 0.75,
                py: 0.25,
                borderRadius: 0.5,
                fontSize: '11px',
                lineHeight: 1.2,
                color: 'common.white',
                backgroundColor: 'rgba(0, 0, 0, 0.78)',
                pointerEvents: 'none',
                whiteSpace: 'nowrap',
                zIndex: 2,
              }}
            >
              {new Date(hoveredTs).toLocaleTimeString()}
            </Box>
          ) : null}

          <Box
            onMouseMove={onBarMouseMove}
            onMouseLeave={onBarMouseLeave}
            sx={{
              width: '100%',
              height: 20,
              borderRadius: 1,
              overflow: 'hidden',
              display: 'flex',
              border: '1px solid #4d4d4d',
              position: 'relative',
              cursor: 'crosshair',
            }}
          >
            {segments.map((segment, idx) => {
              const width = ((segment.end - segment.start) / total) * 100
              const indication = normalizeIndication(segment.indication)
              return (
                <Box
                  key={`${label}-${idx}-${segment.start}`}
                  sx={{
                    width: `${Math.max(width, 0.5)}%`,
                    backgroundColor: SIGNAL_COLORS[indication] ?? SIGNAL_COLORS.UNKNOWN,
                  }}
                  title={`${indication}: ${new Date(segment.start).toLocaleTimeString()} - ${new Date(segment.end).toLocaleTimeString()}`}
                />
              )
            })}

            {hoveredTs !== null ? (
              <Box
                sx={{
                  position: 'absolute',
                  left: `${hoverPercent * 100}%`,
                  top: 0,
                  bottom: 0,
                  width: '1px',
                  backgroundColor: 'common.white',
                  opacity: 0.85,
                  pointerEvents: 'none',
                }}
              />
            ) : null}
          </Box>

          <Box sx={{ position: 'relative', height: 8, mt: 0.25 }}>
            {ticks.map((tick) => (
              <Box
                key={`${label}-tick-${tick}`}
                sx={{
                  position: 'absolute',
                  left: `${tick * 100}%`,
                  top: 0,
                  width: '1px',
                  height: 6,
                  backgroundColor: 'text.secondary',
                  opacity: 0.7,
                  transform: 'translateX(-0.5px)',
                }}
              />
            ))}
          </Box>
        </Box>
      </Box>
    </Box>
  )
}

const AtspmComparison = ({ token, intersectionId, startTime, endTime, selectedFeature }: AtspmComparisonProps) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [latestLog, setLatestLog] = useState<AtspmSpatPairLog | null>(null)

  const selectedSignalGroup = Number(selectedFeature?.feature?.properties?.signalGroupId)
  const isConnectingLane = selectedFeature?.feature?.layer?.id === 'connecting-lanes'
  const showComparisonBars = EnvironmentVars.ENABLE_ATSPM_COMPARISON_BARS

  useEffect(() => {
    const load = async () => {
      if (!token || !intersectionId || !isConnectingLane || Number.isNaN(selectedSignalGroup)) {
        setLatestLog(null)
        setError(null)
        return
      }

      setLoading(true)
      setError(null)

      try {
        const logs = await AtspmApi.getSpatPairLogs({
          token,
          intersectionId,
          queryTime: endTime,
          latest: true,
        })

        setLatestLog(logs?.[0] ?? null)
      } catch (e) {
        setError('Failed to load ATSPM comparison data')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [token, intersectionId, startTime?.getTime(), endTime?.getTime(), isConnectingLane, selectedSignalGroup])

  const summary = useMemo(() => {
    const key = String(selectedSignalGroup)
    return latestLog?.signalGroupStatistics?.[key]
  }, [latestLog, selectedSignalGroup])

  const { windowStart, windowEnd, spatSegments, atspmSegments } = useMemo(() => {
    const fallbackStart = parseMillis(latestLog?.startTime) ?? Date.now() - 60000
    const fallbackEnd = parseMillis(latestLog?.endTime) ?? Date.now()
    const start = startTime?.getTime() ?? fallbackStart
    const end = endTime?.getTime() ?? fallbackEnd
    const normalizedStart = Math.min(start, end)
    const normalizedEnd = Math.max(start, end)

    const pairs =
      latestLog?.atspmSpatPairs?.filter((pair) => {
        return sameSignalGroup(pair?.spatSignalGroupId, selectedSignalGroup)
      }) ?? []

    const spatPoints: TimelinePoint[] = pairs
      .map((pair) => ({
        ts: parseMillis(pair.spatTimestamp),
        indication: normalizeIndication(pair.spatIndication || pair.spatMovementPhaseState),
      }))
      .filter((v): v is TimelinePoint => v.ts !== undefined)

    const atspmPoints: TimelinePoint[] = pairs
      .map((pair) => ({
        ts: parseMillis(pair.atspmTimestamp),
        indication: normalizeIndication(pair.atspmEventCode),
      }))
      .filter((v): v is TimelinePoint => v.ts !== undefined)

    return {
      windowStart: normalizedStart,
      windowEnd: normalizedEnd,
      spatSegments: buildSegments(spatPoints, normalizedStart, normalizedEnd),
      atspmSegments: buildSegments(atspmPoints, normalizedStart, normalizedEnd),
    }
  }, [latestLog, selectedSignalGroup, startTime?.getTime(), endTime?.getTime()])

  if (!isConnectingLane) {
    return <Typography fontSize="13px">Select a connecting lane to view ATSPM vs SPaT comparison.</Typography>
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CircularProgress size={18} />
        <Typography fontSize="13px">Loading ATSPM data...</Typography>
      </Box>
    )
  }

  if (error) {
    return (
      <Typography color="error" fontSize="13px">
        {error}
      </Typography>
    )
  }

  if (!latestLog) {
    return <Typography fontSize="13px">No ATSPM pair log data found for this query window.</Typography>
  }

  return (
    <Box>
      <Typography fontSize="13px" sx={{ mb: 1 }}>
        Signal Group: {selectedSignalGroup}
      </Typography>
      {summary ? (
        <Typography fontSize="12px" sx={{ mb: 1.5 }}>
          Paired: {summary.percentAllPaired.toFixed(1)}% (Green {summary.percentGreenPaired.toFixed(1)}%, Yellow{' '}
          {summary.percentYellowPaired.toFixed(1)}%, Red {summary.percentRedPaired.toFixed(1)}%)
        </Typography>
      ) : null}

      {showComparisonBars ? (
        <>
          <TimelineBar label="SPaT" segments={spatSegments} windowStart={windowStart} windowEnd={windowEnd} />
          <TimelineBar label="ATSPM" segments={atspmSegments} windowStart={windowStart} windowEnd={windowEnd} />

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
            <Typography fontSize="11px" color="text.secondary">
              {new Date(windowStart).toLocaleTimeString()}
            </Typography>
            <Typography fontSize="11px" color="text.secondary">
              {new Date(windowEnd).toLocaleTimeString()}
            </Typography>
          </Box>
        </>
      ) : (
        <Typography fontSize="12px" color="text.secondary">
          ATSPM comparison bars are disabled.
        </Typography>
      )}
    </Box>
  )
}

export default AtspmComparison

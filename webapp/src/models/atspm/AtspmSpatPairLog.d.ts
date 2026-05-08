type AtspmSignalIndication = 'RED' | 'YELLOW' | 'GREEN' | string

type AtspmSpatPair = {
  atspmTimestamp?: string | number
  atspmPrimaryPhase?: number
  atspmSecondaryPhase?: number
  atspmEventCode?: AtspmSignalIndication
  spatTimestamp?: string | number
  spatSignalGroupId?: number
  spatMovementPhaseState?: string
  spatIndication?: AtspmSignalIndication
  isPaired?: boolean
}

type AtspmSignalGroupStatistics = {
  signalGroup: number
  percentGreenPaired: number
  percentYellowPaired: number
  percentRedPaired: number
  percentAllPaired: number
}

type AtspmSpatPairLog = {
  routeId: number
  signalId: string
  intersectionId: number
  startTime: string | number
  endTime: string | number
  atspmSpatPairs: AtspmSpatPair[]
  error?: string
  percentPaired?: number
  percentGreenPaired?: number
  percentRedPaired?: number
  percentYellowPaired?: number
  signalGroupStatistics?: Record<string, AtspmSignalGroupStatistics>
}

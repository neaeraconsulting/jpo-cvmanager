import { useEffect, useState } from 'react'
import { Button, FormControl, InputLabel, MenuItem, Select, Stack, Typography } from '@mui/material'
import toast from 'react-hot-toast'
import RsuApi from '../apis/intersections/rsu-api'

type RsuModeMenuProps = {
  rsuIp: string
  token?: string
}

const MODE_OPTIONS = [
  { value: 2, label: 'Standby (2)' },
  { value: 4, label: 'Operate (4)' },
]

const getModeLabel = (mode: number | null) => {
  if (mode === 2) return 'Standby (2)'
  if (mode === 4) return 'Operate (4)'
  if (mode === 16) return 'Off (16)'
  if (mode == null) return 'Unknown'
  return `Unknown (${mode})`
}

const RsuModeMenu = ({ rsuIp, token }: RsuModeMenuProps) => {
  const [mode, setMode] = useState<number>(4)
  const [loading, setLoading] = useState<boolean>(false)
  const [currentMode, setCurrentMode] = useState<number | null>(null)
  const [currentModeLoading, setCurrentModeLoading] = useState<boolean>(false)

  const fetchCurrentStatus = async () => {
    if (!token) {
      setCurrentMode(null)
      return
    }

    setCurrentModeLoading(true)
    try {
      const response = await RsuApi.getCurrentRsuModeStatus({ token, rsuIp })
      if (response?.mode != null) {
        setCurrentMode(response.mode)
      } else {
        setCurrentMode(null)
      }
    } finally {
      setCurrentModeLoading(false)
    }
  }

  useEffect(() => {
    fetchCurrentStatus()
  }, [rsuIp, token])

  const handleSetMode = async () => {
    if (!token) {
      toast.error('Unable to set RSU status: missing authentication token.')
      return
    }

    setLoading(true)
    try {
      const response = await RsuApi.setRsuMode({ token, rsuIp, mode })
      if (response?.status?.toLowerCase() === 'success') {
        toast.success(response.message ?? 'RSU status updated successfully.')
        await fetchCurrentStatus()
      } else {
        toast.error(response?.message ?? 'Failed to update RSU status.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack spacing={2}>
      <Typography>Set the current operating status of this RSU.</Typography>
      <Typography>
        Current Status: {currentModeLoading ? 'Loading...' : <strong>{getModeLabel(currentMode)}</strong>}
      </Typography>
      <FormControl fullWidth>
        <InputLabel id="rsu-mode-select-label">RSU Status</InputLabel>
        <Select
          labelId="rsu-mode-select-label"
          id="rsu-mode-select"
          value={mode}
          label="RSU Status"
          onChange={(event) => setMode(Number(event.target.value))}
        >
          {MODE_OPTIONS.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <Button
        className="museo-slab capital-case"
        variant="contained"
        size="medium"
        onClick={handleSetMode}
        disabled={loading || !token}
        sx={{ width: 'fit-content' }}
      >
        {loading ? 'Updating...' : 'Set RSU Status'}
      </Button>
    </Stack>
  )
}

export default RsuModeMenu

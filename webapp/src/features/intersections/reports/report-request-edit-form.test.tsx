import React from 'react'
import { render } from '@testing-library/react'
import { ReportRequestEditForm } from './report-request-edit-form'
import { Provider } from 'react-redux'
import { ThemeProvider } from '@mui/material'
import { testTheme } from '../../../styles'
import { setupStore } from '../../../store'
import { MockLocalizationProvider, replaceChaoticIds } from '../../../utils/test-utils'

// // Mock the @mui/x-date-pickers module
jest.mock('@mui/x-date-pickers', () => {
  const actual = jest.requireActual('@mui/x-date-pickers')
  return {
    ...actual,
    LocalizationProvider: MockLocalizationProvider,
  }
})

// Mock the dayjs library
jest.mock('dayjs', () => {
  const actualDayjs = jest.requireActual('dayjs')
  const mockDayjs = (date) => actualDayjs(date).tz('America/Denver')
  mockDayjs.extend = actualDayjs.extend
  mockDayjs.Ls = actualDayjs.Ls
  return mockDayjs
})

// Mock a specific timestamp that will display consistently across timezones
// Using a UTC timestamp ensures Date.now() returns the same value everywhere
// This timestamp represents: April 6, 2025 7:00 PM Denver (April 7, 2025 01:00 UTC in MDT)
const MOCK_TIMESTAMP = new Date('2025-04-07T01:00:00.000Z').getTime()
jest.spyOn(Date, 'now').mockImplementation(() => MOCK_TIMESTAMP)
jest.useFakeTimers().setSystemTime(MOCK_TIMESTAMP)

it('should take a snapshot', () => {
  const { container } = render(
    <ThemeProvider theme={testTheme}>
      <Provider store={setupStore({})}>
        <ReportRequestEditForm onGenerateReport={() => {}} dbIntersectionId={0} />
      </Provider>
    </ThemeProvider>
  )

  expect(replaceChaoticIds(container)).toMatchSnapshot()
})

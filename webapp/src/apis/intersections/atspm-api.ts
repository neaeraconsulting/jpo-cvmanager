import { authApiHelper } from './api-helper-cviz'

class AtspmApi {
  async getSpatPairLogs({
    token,
    intersectionId,
    queryTime,
    latest,
    page,
    size,
    abortController,
  }: {
    token: string
    intersectionId: number
    queryTime?: Date
    latest?: boolean
    page?: number
    size?: number
    abortController?: AbortController
  }): Promise<AtspmSpatPairLog[]> {
    const queryParams: Record<string, string> = {
      intersection_id: intersectionId.toString(),
    }

    if (queryTime) queryParams['query_time_utc_millis'] = queryTime.getTime().toString()
    if (latest !== undefined) queryParams['latest'] = latest.toString()
    if (page !== undefined) queryParams['page'] = page.toString()
    if (size !== undefined) queryParams['size'] = size.toString()

    const response: PagedResponse<AtspmSpatPairLog> = await authApiHelper.invokeApi({
      path: '/data/atspm/spat-pair',
      token,
      queryParams,
      abortController,
      failureMessage: 'Failed to retrieve ATSPM SPaT pair logs',
      tag: 'intersection',
      toastOnFailure: false,
    })

    return response?.content ?? []
  }
}

export default new AtspmApi()

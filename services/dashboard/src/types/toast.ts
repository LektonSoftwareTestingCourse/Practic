export type ToastType = 'SUCCESS' | 'ERROR' | 'WARNING' | 'INFO'

export type Toast  = {
    id: string
    message: string
    type: ToastType
    duration?: number
}

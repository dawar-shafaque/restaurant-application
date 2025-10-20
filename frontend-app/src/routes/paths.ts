export enum Paths {
    LANDING='/',
    LOGIN='/login',
    SIGNUP='/signup',
    BOOKTABLE='/bookTable',
    RESERVATION='/reservation',
    LOCATION='/location',
    MYPROFILE='/profile',
    WAITER_RESERVATION='/waiter-reservation',
    VIEW_MENU='/view-menu'
}
export const getLocationPath = (id: string | number) => `${Paths.LOCATION}/${id}`;
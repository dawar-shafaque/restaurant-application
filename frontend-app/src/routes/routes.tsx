import { FC, lazy, LazyExoticComponent } from "react";
import { Paths } from "./paths";
import { Roles } from "@/types/FormData";


type RouteObject = {
  element: LazyExoticComponent<FC>;
  path: string;
  roles?: Roles[];
};
export const routes: readonly RouteObject[] = [
  {
    element: lazy(() => import("@/pages/Landing")),
    path: Paths.LANDING,
  },
  {
    element: lazy(() => import("@/pages/Login")),
    path: Paths.LOGIN,
  },
  {
    element: lazy(() => import("@/pages/Signup")),
    path: Paths.SIGNUP,
  },
  {
    element: lazy(() => import("@/pages/BookTable")),
    path: Paths.BOOKTABLE,
  },
  {
    element: lazy(() => import("@/pages/LocationDetails")),
    path: `${Paths.LOCATION}/:id`,
  },
  {
    element: lazy(() => import("@/pages/CustomerReservation")),
    path: Paths.RESERVATION,
  },
  {
    element: lazy(() => import("@/pages/UserProfile")),
    path: Paths.MYPROFILE,
  },
  {
    element: lazy(() => import("@/pages/waiter/Reservations")),
    path: Paths.WAITER_RESERVATION,
  },
  {
    element: lazy(() => import('@/pages/ViewMenu')),
    path: Paths.VIEW_MENU,
  },
];

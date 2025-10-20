export interface FormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
}
export interface FormErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

export type ReservationStatus =
  | "RESERVED"
  | "IN_PROGRESS"
  | "FINISHED"
  | "CANCELLED";

export interface Reservation {
  id: string;
  location: string;
  date: string;
  timeFrom: string;
  timeTo: string;
  guestsNumber: number;
  status: ReservationStatus;
  locationAddress: string;
  locationId: string;
  waiterEmail: string;
  reservationId: string;
}
export type Review = {
  id: string;
  userName: string;
  userAvatarUrl: string;
  rate: number;
  date: string;
  comment: string;
};

export type UserProfileData = {
  firstName: string;
  lastName: string;
  userAvatarUrl: string;
};

export interface Table {
  id: number;
  locationAddress: string;
  seating: number;
  availableSlots: string[];
  image: string;
  tableNumber: string;
  locationId: string;
  guestCapacity: string;
}

export interface WaiterReservation {
  reservationId: string;
  location: string;
  tableNumber: string;
  date: string;
  timeSlot: string;
  customerName: string;
  guestsNumber: number;
  timeFrom:string;
  timeTo: string;
}

export interface FeedbackData {
  serviceRating: number;
  serviceComment: string;
  cuisineRating: number;
  cuisineComment: string;
}

export enum Roles {
  ADMIN = 'Admin',
  WAITER = 'Waiter',
  CUSTOMER = 'Customer',
}

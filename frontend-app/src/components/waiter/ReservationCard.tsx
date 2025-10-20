import { WaiterReservation } from "@/types/FormData";
import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar } from "lucide-react";
import { FaMapMarkerAlt, FaUserFriends } from "react-icons/fa";
import { MdAccessTime } from "react-icons/md";
import { format } from "date-fns";

interface Props {
  reservation: WaiterReservation;
  onCancel: (id: string) => void;
  onPostpone?: () => void;
}

export const ReservationCard: React.FC<Props> = ({
  reservation,
  onCancel,
  onPostpone,
}) => {
  return (
    <>
      <Card className="flex flex-col justify-between h-full">
        <CardContent className="p-4 space-y-2">
          <div className="flex justify-between items-center">
            <div className="flex items-center text-green-600 font-medium">
              <FaMapMarkerAlt className="mr-2" />
              {reservation.location}
            </div>
            <div className="text-sm text-gray-700 font-semibold">
              {reservation.tableNumber}
            </div>
          </div>

          <div className="text-gray-600">
            <div className="flex items-center">
              <Calendar className="w-4 h-4 mr-2" />
              {format(reservation.date, "yyyy-MM-dd")}
            </div>
            <div className="flex items-center">
              <MdAccessTime className="w-4 h-4 mr-2" />
              {reservation.timeSlot}
            </div>
          </div>

          <div>
            <p className="font-medium text-gray-800">
              {reservation.customerName}
            </p>
            <div className="flex items-center text-sm text-gray-600 mt-1">
              <FaUserFriends className="mr-2" />
              {reservation.guestsNumber}
            </div>
          </div>
        </CardContent>

        <div className="flex justify-between border-t px-4 py-3 mt-auto">
          <Button
            variant="ghost"
            className="text-gray-500 hover:text-gray-700"
            onClick={() => onCancel(reservation.reservationId)}
          >
            Cancel
          </Button>
          <Button
            variant="outline"
            className="border-green-600 text-green-600 hover:bg-green-50"
            onClick={onPostpone}
          >
            Postpone
          </Button>
        </div>
      </Card>
    </>
  );
};

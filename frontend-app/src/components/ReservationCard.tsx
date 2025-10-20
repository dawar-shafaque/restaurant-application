import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Reservation } from "@/types/FormData";
import { cn } from "@/lib/utils";
import { useSelector } from "react-redux";
import { RootState } from "@/redux/store";
 
interface ReservationCardProps {
  reservation: Reservation;
  onEdit?: () => void;
  onCancel?: () => void;
  onFeedback?: () => void;
  locationId?: string
}

 
const statusColors: Record<Reservation["status"], string> = {
  RESERVED: "bg-yellow-200 text-yellow-700",
  IN_PROGRESS: "bg-blue-200 text-blue-700",
  FINISHED: "bg-green-200 text-green-700",
  CANCELLED: "bg-red-200 text-red-700",
};
 
export default function ReservationCard({ reservation, onEdit, onCancel, onFeedback, locationId }: ReservationCardProps) {
  const location = useSelector((state: RootState) => state.locations.locations);
  const locationDetails = location.find((loc) => loc.id === locationId);
  return (
    <Card className="w-full max-w-sm">
      <CardHeader className="flex justify-between">
        <div>
          <h3 className="text-lg font-semibold">{locationDetails?.address}</h3>
          <Badge className={cn("px-3 py-1", statusColors[reservation.status])}>
            {reservation.status}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p data-testid="date"><strong >Date:</strong> {reservation.date}</p>
        <p data-testid="time"><strong>Time:</strong> {reservation.timeFrom} - {reservation.timeTo}</p>
        <p data-testid="guestCount"><strong>Guests:</strong> {reservation.guestsNumber}</p>
      </CardContent>
      <CardFooter className="flex space-x-2">
        {reservation.status === "RESERVED" && (
          <>
            <Button variant="outline" onClick={onCancel}>Cancel</Button>
            <Button onClick={onEdit} className="cursor-pointer">Edit</Button>
          </>
        )}
        {reservation.status === "FINISHED" && (
          <Button onClick={onFeedback}>Update Feedback</Button>
        )}
        {reservation.status === "IN_PROGRESS" && (
          <Button onClick={onFeedback}>Leave Feedback</Button>
        )}
      </CardFooter>
    </Card>
  );
}
 
 
import { ReservationCard } from "@/components/waiter/ReservationCard";
import { ReservationFilter } from "@/components/waiter/ReservationFilter";
import { Navbar } from "@/components/Navbar";
import { API } from "@/api/endpoints";
import { useState } from "react";
import { WaiterReservation } from "@/types/FormData";
import { toast } from "react-toastify";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { DialogTitle } from "@radix-ui/react-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const ReservationDashboard: React.FC = () => {
  const [reservations, setReservations] = useState<WaiterReservation[]>([]);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [id, setId] = useState("");
  const [editForm, setEditForm] = useState({
    // date: "",
    timeFrom: "",
    timeTo: "",
    guestsNumber: "",
  });

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setEditForm((prevForm) => ({
      ...prevForm,
      [name]: value,
    }));
  };

  const handleSearch = async (
    date: string,
    time: string,
    tableNumber: string
  ) => {
    console.log(date, time, tableNumber);
    try {
      const response = await fetch(
        `${API.RESERVATIONS}?date=${date}&time=${time}&tableNumber=${tableNumber}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem("token")}`,
          },
        }
      );
      const responseData = await response.json();
      const sortedReservations = responseData.sort(
        (a: WaiterReservation, b: WaiterReservation) =>
          new Date(a.date).getTime() - new Date(b.date).getTime()
      );
      setReservations(sortedReservations);
      console.log(responseData);
    } catch (error) {
      console.error(error);
    }
  };
  const handleReservationCancel = async (id: string) => {
    try {
      const response = await fetch(`${API.RESERVATIONS}/${id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
      });
      const responseData = await response.text();
      if (!response.ok) {
        toast.error(responseData);
        throw new Error("Failed to delete the reservation");
      } else toast.success(responseData);
      console.log(responseData);
    } catch (error) {
      console.error(error);
    }
  };
  const handleEditSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const response = await fetch(`${API.RESERVATIONS}/${id}`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${sessionStorage.getItem("token")}`,
      },
      body: JSON.stringify(editForm)
    });
    const responseData = await response.json();
    if (!response.ok) {
      toast.error(responseData.message);
      throw new Error("Failed to edit the reservation");
    } else toast.success(responseData.message);
    setIsDialogOpen(false)
    console.log(responseData)
  };
  return (
    <>
      <Navbar />
      <div className="p-6 space-y-6">
        <ReservationFilter onSearch={handleSearch} />

        <p className="text-lg font-medium">
          You have {reservations.length}
          {reservations.length > 1 ? " reservations" : " reservation"}
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {reservations.map((reservation) => (
            <ReservationCard
              key={reservation.reservationId}
              reservation={reservation}
              onCancel={handleReservationCancel}
              onPostpone={() => {
                const [timeFrom, timeTo] = reservation.timeSlot.split(" - ");
                setId(reservation.reservationId)
                setEditForm({
                  timeFrom,
                  timeTo,
                  guestsNumber: reservation.guestsNumber
                    .toString()
                    .split(" ")[0],
                });
                setIsDialogOpen(true);
              }}
            />
          ))}
        </div>
      </div>
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reservation Details</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleEditSubmit}>
            <div className="flex items-center gap-8">
              <div className="mb-4">
                <Label
                  htmlFor="timeFrom"
                  className="mb-2 text-md text-gray-700"
                >
                  Time From
                </Label>
                <Input
                  type="time"
                  name="timeFrom"
                  id="timeFrom"
                  value={editForm.timeFrom}
                  onChange={handleFormChange}
                />
              </div>
              <div className="mb-4">
                <Label htmlFor="timeTo" className="mb-2 text-md text-gray-700">
                  Time To
                </Label>
                <Input
                  type="time"
                  name="timeTo"
                  id="timeTo"
                  value={editForm.timeTo}
                  onChange={handleFormChange}
                />
              </div>
            </div>
            <div>
              <Label
                htmlFor="guestsNumber"
                className="mb-2 text-md text-gray-700"
              >
                Number of Guests
              </Label>
              <Input
                type="number"
                name="guestsNumber"
                id="guestsNumber"
                value={editForm.guestsNumber}
                min="1"
                onChange={handleFormChange}
              />
            </div>
            <div className="mt-4 flex justify-end">
              <Button
                type="submit"
                className="cursor-pointer bg-green-500 text-md font-semibold"
              >
                Confirm Postpone
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default ReservationDashboard;

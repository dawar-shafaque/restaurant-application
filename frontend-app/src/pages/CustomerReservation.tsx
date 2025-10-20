import { useEffect, useState } from "react";
import ReservationCard from "@/components/ReservationCard";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Navbar } from "@/components/Navbar";
import { FeedbackData, Reservation } from "@/types/FormData";
import { toast } from "react-toastify";
import { API } from "@/api/endpoints";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import FeedbackModal from "@/components/Feedback";

export default function ReservationsPage() {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(false); // <-- loading state
  const [selectedReservation, setSelectedReservation] =
    useState<Reservation | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editForm, setEditForm] = useState({
    // date: "",
    timeFrom: "",
    timeTo: "",
    guestsNumber: "",
  });
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);

  const handleEdit = (reservation: Reservation) => {
    setSelectedReservation(reservation);
    setEditForm({
      // date: reservation.date,
      timeFrom: reservation.timeFrom,
      timeTo: reservation.timeTo,
      guestsNumber: reservation.guestsNumber.toString(),
    });
    setIsDialogOpen(true);
  };

  const handleCancel = async (id: string) => {
    try {
      const URL = `${API.DELETE_RESERVATION}/${id}`;

      const deleteResponse = await fetch(URL, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
      });

      if (!deleteResponse.ok) {
        throw new Error("Failed to delete the reservation");
      }

      toast.success("Reservation Cancelled successfully!");

      // Refetch reservations
      const getResponse = await fetch(API.RESERVATIONS, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
      });

      if (!getResponse.ok) {
        throw new Error("Failed to fetch reservations");
      }

      const responseData = await getResponse.json();
      const data = Array.isArray(responseData)
        ? responseData
        : responseData.data || [];

      setReservations(data);
    } catch (error) {
      console.error("Failed to delete reservation:", error);
    }
  };

  const handleFeedbackSubmit = async (feedback: FeedbackData) => {

    const feedbackData = {
      reservationId: selectedReservation?.id,
      ...feedback,
    };
    try {
      const response = await fetch(API.FEEDBACKS_POST, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
        body: JSON.stringify(feedbackData),
      });
      const responseData = await response.json();
      if (!response.ok) {
        toast.error(responseData.message);
      } else toast.success(responseData.message);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "An unexpected error occurred";
      toast.error(errorMessage);
    }
  };

  useEffect(() => {
    const FetchResevations = async () => {
      setLoading(true);
      try {
        const response = await fetch(API.RESERVATIONS, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem("token")}`,
          },
        });
        const responseData = await response.json();
        
        const data = Array.isArray(responseData)
          ? responseData
          : responseData.data || [];

            data.sort((a: Reservation, b: Reservation) => {
            if (a.date !== b.date) {
              return new Date(a.date).getTime() - new Date(b.date).getTime();
            } else {
              return a.timeFrom.localeCompare(b.timeFrom);
            }
            });
        setReservations(data);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    };
    FetchResevations();
  }, []);

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setEditForm((prevForm) => ({
      ...prevForm,
      [name]: value,
    }));
  };

  const handleEditSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const response = await fetch(
      `${API.RESERVATIONS}/${selectedReservation?.id}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
        body: JSON.stringify(editForm),
      }
    );
    const responseData = await response.json();
    if (!response.ok) {
      toast.error(responseData.message);
    } else toast.success("Reservation updated successfully!");
    setIsDialogOpen(false);

    const FetchResevations = async () => {
      setLoading(true);
      try {
        const response = await fetch(API.RESERVATIONS, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem("token")}`,
          },
        });
        const responseData = await response.json();

        const data = Array.isArray(responseData)
          ? responseData
          : responseData.data || [];
        setReservations(data);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    };
    FetchResevations();
  };

  return (
    <div className="bg-background min-h-screen">
      <Navbar />
      <div className="container mx-auto p-6">
        <h1 className="text-2xl font-bold mb-4">
          Hello, {sessionStorage.getItem("username")} (
          {sessionStorage.getItem("role")})
        </h1>

        {loading ? (
          <div className="flex justify-center py-10">
            <div className="animate-spin rounded-full h-10 w-10 border-t-4 border-blue-500 " />
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {reservations.map((reservation) => (
              <ReservationCard
                key={reservation.id}
                reservation={reservation}
                onEdit={() => handleEdit(reservation)}
                onCancel={() => handleCancel(reservation.id)}
                onFeedback={() => {
                  setSelectedReservation(reservation);
                  setIsFeedbackOpen(true);
                }}
                locationId={reservation.locationId}
              />
            ))}
          </div>
        )}
      </div>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Reservation</DialogTitle>
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
                Save Changes
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
      {selectedReservation && (
        <FeedbackModal
          open={isFeedbackOpen}
          onClose={() => setIsFeedbackOpen(false)}
          onSubmit={handleFeedbackSubmit}
          reservation={selectedReservation}
        />
      )}
    </div>
  );
}

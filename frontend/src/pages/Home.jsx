import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';

export default function Home() {
  const [events, setEvents] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchEvents = async (search = '') => {
    setLoading(true);
    try {
      const params = search ? { keyword: search } : {};
      const res = await api.get('/events/search', { params });
      setEvents(res.data.content);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchEvents(); }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchEvents(keyword);
  };

  return (
    <div className="max-w-5xl mx-auto px-6 py-8">
      <h1 className="text-3xl font-bold mb-6">Find Events</h1>

      <form onSubmit={handleSearch} className="flex gap-3 mb-8">
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search by event, artist, venue..."
          className="flex-1 px-4 py-2 rounded-lg bg-gray-800 border border-gray-700 focus:outline-none focus:border-purple-500"
        />
        <button type="submit" className="px-6 py-2 bg-purple-600 rounded-lg hover:bg-purple-700">
          Search
        </button>
      </form>

      {loading ? (
        <p className="text-gray-400">Loading...</p>
      ) : events.length === 0 ? (
        <p className="text-gray-400">No events found.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {events.map(event => (
            <div
              key={event.id}
              onClick={() => navigate(`/event/${event.id}`)}
              className="bg-gray-900 border border-gray-800 rounded-lg p-5 cursor-pointer hover:border-purple-500 transition"
            >
              <h3 className="text-lg font-semibold mb-1">{event.name}</h3>
              <p className="text-sm text-gray-400 mb-2">{event.description}</p>
              <p className="text-sm text-purple-400">{event.performer?.name}</p>
              <p className="text-sm text-gray-500">{event.venue?.name}</p>
              <p className="text-sm text-gray-500 mt-2">
                {new Date(event.eventDate).toLocaleDateString('en-US', {
                  weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                })}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}